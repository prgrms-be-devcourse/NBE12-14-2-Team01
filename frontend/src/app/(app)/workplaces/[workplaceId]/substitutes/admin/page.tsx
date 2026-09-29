"use client";

import { useEffect, useState, useCallback } from "react";
import Card from "@/components/ui/Card";
import { useParams } from "next/navigation";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch, ApiError } from "@/lib/api";

export type RequestStatus = "OPEN" | "ACCEPTED" | "APPROVED" | "CLOSED";

// DTO
export type AcceptedMemberResponse = {
  memberId: number;
  name: string;
};

export type SubstituteRequestItem = {
  requestId: number;
  shiftId: number;
  requesterMemberId: number;
  requesterName: string;
  workplaceId: number;
  workplaceName: string;
  startAt: string;
  endAt: string;
  status: RequestStatus;
  createdAt: string;
  acceptedMember: AcceptedMemberResponse | null;
};

type Tab = "WAITING" | "APPROVAL" | "COMPLETED";

// 서버의 LocalDateTime 문자열을 KST(UTC+9) Date 객체로 해석
function parseAsKst(isoString: string | null): Date | null {
  if (!isoString) return null;
  const hasTimeZone = /[Z+-]\d{2}:?\d{2}$/.test(isoString);
  const kstFormattedString = hasTimeZone ? isoString : `${isoString}+09:00`;
  return new Date(kstFormattedString);
}

// KST 시간 포맷팅 (HH:mm)
function formatKstTime(date: Date): string {
  const formatter = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  const parts = formatter.formatToParts(date);
  const hour = parts.find((p) => p.type === "hour")?.value || "00";
  const minute = parts.find((p) => p.type === "minute")?.value || "00";
  return `${hour}:${minute}`;
}

// 대체 근무 일시 포맷팅 (YYYY.MM.DD (요일) / HH:mm - HH:mm)
function formatShiftDateTime(startAtStr: string, endAtStr: string) {
  const startDate = parseAsKst(startAtStr);
  const endDate = parseAsKst(endAtStr);

  if (!startDate || !endDate) {
    return { date: "-", day: "-", timeRange: "-", hours: 0 };
  }

  const formatter = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  });

  const parts = formatter.formatToParts(startDate);
  const year = parts.find((p) => p.type === "year")?.value || "";
  const month = parts.find((p) => p.type === "month")?.value || "";
  const dayNum = parts.find((p) => p.type === "day")?.value || "";
  const dayName = parts.find((p) => p.type === "weekday")?.value || "";

  const startTime = formatKstTime(startDate);
  const endTime = formatKstTime(endDate);

  const workHours = getWorkHours(startTime, endTime);

  return {
    date: `${year}.${month}.${dayNum}`,
    day: dayName,
    timeRange: `${startTime} - ${endTime}`,
    hours: workHours,
  };
}

// 근무 시간 계산 (자정 넘어가는 밤샘 근무 예외 처리 보장)
function getWorkHours(startTime: string, endTime: string): number {
  const [startHour, startMinute] = startTime.split(":").map(Number);
  const [endHour, endMinute] = endTime.split(":").map(Number);

  const startMins = startHour * 60 + startMinute;
  let endMins = endHour * 60 + endMinute;

  if (endMins < startMins) {
    endMins += 24 * 60;
  }

  return (endMins - startMins) / 60;
}

// 이름 첫 글자 추출
function getInitial(name: string | null | undefined) {
  return name ? name.trim().slice(0, 1) : "";
}

export default function SubstituteAdminPage() {
  const routeParams = useParams();
  const workplaceId = routeParams?.workplaceId as string;

  const [requests, setRequests] = useState<SubstituteRequestItem[]>([]);
  const [tab, setTab] = useState<Tab>("APPROVAL");
  const [selectedRequest, setSelectedRequest] =
      useState<SubstituteRequestItem | null>(null);
  const [isLoading, setIsLoading] = useState(() => Boolean(workplaceId));
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!workplaceId) return;

    let isIgnore = false;

    const loadRequests = async () => {
      setIsLoading(true);
      try {
        const data = await apiFetch<SubstituteRequestItem[]>(
            `/workplaces/${workplaceId}/substitute-requests/ongoing`
        );
        if (!isIgnore) {
          setRequests(data ?? []);
        }
      } catch (error) {
        if (!isIgnore) {
          if (error instanceof ApiError) {
            alert(`[${error.code}] ${error.message}`);
          } else {
            console.error("대체 근무 요청 목록 조회 실패:", error);
          }
        }
      } finally {
        if (!isIgnore) {
          setIsLoading(false);
        }
      }
    };

    loadRequests();

    return () => {
      isIgnore = true;
    };
  }, [workplaceId]);

  // 최종 승인 처리
  const handleApprove = useCallback(async (requestId: number) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    try {
      await apiFetch(`/substitute-requests/${requestId}/approve`, {
        method: "PATCH",
      });

      setRequests((prev) =>
          prev.map((req) =>
              req.requestId === requestId
                  ? { ...req, status: "APPROVED" as RequestStatus }
                  : req
          )
      );
      setSelectedRequest(null);
      alert("대체 근무 요청이 최종 승인되었습니다.");
      setTab("COMPLETED");
    } catch (error) {
      if (error instanceof ApiError) {
        alert(`[${error.code}] ${error.message}`);
      } else {
        alert("승인 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }, [isSubmitting]);

  // 요청 종료 처리
  const handleClose = useCallback(async (requestId: number) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    try {
      await apiFetch(`/substitute-requests/${requestId}/close`, {
        method: "PATCH",
      });

      setRequests((prev) =>
          prev.map((req) =>
              req.requestId === requestId
                  ? { ...req, status: "CLOSED" as RequestStatus }
                  : req
          )
      );
      setSelectedRequest(null);
      alert("요청이 종료되었습니다.");
      setTab("COMPLETED");
    } catch (error) {
      if (error instanceof ApiError) {
        alert(`[${error.code}] ${error.message}`);
      } else {
        alert("종료 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }, [isSubmitting]);

  // 탭 카운트
  const waitingCount = requests.filter((r) => r.status === "OPEN").length;
  const approvalCount = requests.filter((r) => r.status === "ACCEPTED").length;
  const completedCount = requests.filter(
      (r) => r.status === "APPROVED" || r.status === "CLOSED"
  ).length;

  // 탭 필터링
  const filteredRequests = requests.filter((request) => {
    if (tab === "WAITING") return request.status === "OPEN";
    if (tab === "APPROVAL") return request.status === "ACCEPTED";
    return request.status === "APPROVED" || request.status === "CLOSED";
  });

  return (
      <>
        <PageHeader
            title="승인 관리"
            description="직원이 수락한 대체 근무 요청을 확인하고 최종 승인하세요."
        />

        {/* 탭 필터 버튼 */}
        <div className="mb-5 flex flex-wrap gap-2">
          <button
              type="button"
              onClick={() => setTab("WAITING")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "WAITING"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            수락 대기 {waitingCount}
          </button>

          <button
              type="button"
              onClick={() => setTab("APPROVAL")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "APPROVAL"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            승인 필요 {approvalCount}
          </button>

          <button
              type="button"
              onClick={() => setTab("COMPLETED")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "COMPLETED"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            처리 완료 {completedCount}
          </button>
        </div>

        <Card>
          <div className="overflow-x-auto">
            <div className="min-w-[850px]">
              <div className="grid grid-cols-[1.25fr_1fr_1fr_1fr_1fr] border-b border-[#edf2ef] px-3 pb-3 text-sm font-bold text-[#78847f]">
                <div>근무 일시</div>
                <div>요청자</div>
                <div>수락자</div>
                <div>상태</div>
                <div className="text-right">관리</div>
              </div>

              {isLoading ? (
                  <div className="flex min-h-[200px] items-center justify-center text-sm text-[#78847f]">
                    요청 목록을 불러오는 중입니다...
                  </div>
              ) : filteredRequests.length === 0 ? (
                  <div className="flex min-h-[200px] items-center justify-center text-sm text-[#78847f]">
                    {tab === "WAITING" && "현재 수락을 기다리는 요청이 없습니다."}
                    {tab === "APPROVAL" && "현재 승인이 필요한 요청이 없습니다."}
                    {tab === "COMPLETED" && "처리 완료된 요청이 없습니다."}
                  </div>
              ) : (
                  filteredRequests.map((request) => {
                    const { date, day, timeRange, hours } = formatShiftDateTime(
                        request.startAt,
                        request.endAt
                    );

                    return (
                        <div
                            key={request.requestId}
                            className="grid min-h-[88px] grid-cols-[1.25fr_1fr_1fr_1fr_1fr] items-center border-b border-[#edf2ef] px-3 last:border-b-0"
                        >
                          <div>
                            <p className="font-black">
                              {date} ({day})
                            </p>
                            <p className="mt-1 text-sm text-[#78847f]">
                              {timeRange}{" "}
                              <span className="text-xs text-[#9aa5a0]">({hours}h)</span>
                            </p>
                          </div>

                          <div className="flex items-center gap-3">
                            <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[#f3fbf7] font-black text-[#005642]">
                              {getInitial(request.requesterName)}
                            </div>
                            <p className="font-bold">{request.requesterName}</p>
                          </div>

                          <div>
                            {request.acceptedMember ? (
                                <div className="flex items-center gap-3">
                                  <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[#dff7ec] font-black text-[#005642]">
                                    {getInitial(request.acceptedMember.name)}
                                  </div>
                                  <p className="font-bold">
                                    {request.acceptedMember.name}
                                  </p>
                                </div>
                            ) : (
                                <p className="text-sm text-[#9aa5a0]">아직 없음</p>
                            )}
                          </div>

                          <div>
                            {request.status === "OPEN" && (
                                <span className="rounded-full bg-[#e7efff] px-3 py-1 text-xs font-bold text-[#4a73c9]">
                          수락 대기
                        </span>
                            )}
                            {request.status === "ACCEPTED" && (
                                <span className="rounded-full bg-[#fff1d7] px-3 py-1 text-xs font-bold text-[#a96d09]">
                          승인 필요
                        </span>
                            )}
                            {request.status === "APPROVED" && (
                                <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                          승인 완료
                        </span>
                            )}
                            {request.status === "CLOSED" && (
                                <span className="rounded-full bg-[#f1f3f2] px-3 py-1 text-xs font-bold text-[#78847f]">
                          종료됨
                        </span>
                            )}
                          </div>

                          <div className="flex justify-end gap-2">
                            {request.status === "ACCEPTED" && (
                                <>
                                  <button
                                      type="button"
                                      onClick={() => handleClose(request.requestId)}
                                      disabled={isSubmitting}
                                      className="rounded-lg border border-[#f1cccc] px-3 py-2 text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5] disabled:opacity-50 disabled:cursor-not-allowed"
                                  >
                                    종료
                                  </button>
                                  <button
                                      type="button"
                                      onClick={() => handleApprove(request.requestId)}
                                      disabled={isSubmitting}
                                      className="rounded-lg bg-[#005642] px-3 py-2 text-sm font-bold text-white transition hover:bg-[#0b6b52] disabled:opacity-50 disabled:cursor-not-allowed"
                                  >
                                    승인
                                  </button>
                                </>
                            )}

                            {(request.status === "OPEN" ||
                                request.status === "APPROVED" ||
                                request.status === "CLOSED") && (
                                <button
                                    type="button"
                                    onClick={() => setSelectedRequest(request)}
                                    className="rounded-lg border border-[#dce8e2] px-3 py-2 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7] disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  상세 보기
                                </button>
                            )}
                          </div>
                        </div>
                    );
                  })
              )}
            </div>
          </div>
        </Card>

        <div className="mt-5 rounded-xl border border-[#dce8e2] bg-[#f3fbf7] p-4">
          <p className="text-sm font-black text-[#005642]">최종 승인 안내</p>
          <p className="mt-1 text-sm leading-6 text-[#66736d]">
            관리자가 승인하면 해당 스케줄의 근무 담당자가 대타를 수락한 직원으로 자동으로 교체됩니다.
          </p>
        </div>

        {/* 상세 보기 모달 */}
        {selectedRequest && (() => {
          const { date, day, timeRange, hours } = formatShiftDateTime(
              selectedRequest.startAt,
              selectedRequest.endAt
          );

          return (
              <div
                  className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4"
                  onClick={() => !isSubmitting && setSelectedRequest(null)}
              >
                <div
                    className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
                    onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h2 className="text-xl font-black">대체 근무 요청 상세</h2>
                      <p className="mt-1 text-sm text-[#78847f]">
                        요청 정보 및 진행 상태를 확인합니다.
                      </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => setSelectedRequest(null)}
                        disabled={isSubmitting}
                        className="text-xl text-[#78847f] hover:text-black disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      ×
                    </button>
                  </div>

                  <div className="mt-6 space-y-4">
                    <div className="rounded-xl bg-[#f3fbf7] p-4">
                      <p className="text-xs font-semibold text-[#78847f]">
                        근무 일시
                      </p>
                      <p className="mt-1 font-black">
                        {date} ({day})
                      </p>
                      <p className="mt-1 text-sm text-[#66736d]">
                        {timeRange} ({hours}시간)
                      </p>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div className="rounded-xl border border-[#dce8e2] p-4">
                        <p className="text-xs text-[#78847f]">요청자</p>
                        <p className="mt-1 font-black">
                          {selectedRequest.requesterName}
                        </p>
                      </div>

                      <div className="rounded-xl border border-[#dce8e2] p-4">
                        <p className="text-xs text-[#78847f]">수락자</p>
                        <p className="mt-1 font-black">
                          {selectedRequest.acceptedMember?.name ?? "아직 없음"}
                        </p>
                      </div>
                    </div>

                    <div>
                      <p className="mb-2 text-sm font-bold">현재 상태</p>
                      {selectedRequest.status === "OPEN" && (
                          <div className="rounded-xl bg-[#eef4ff] px-4 py-3 text-sm font-bold text-[#4a73c9]">
                            후보 직원의 수락 응답을 기다리고 있습니다.
                          </div>
                      )}
                      {selectedRequest.status === "ACCEPTED" && (
                          <div className="rounded-xl bg-[#fff8e8] px-4 py-3 text-sm font-bold text-[#a96d09]">
                            후보가 수락했습니다. 관리자의 최종 승인이 필요합니다.
                          </div>
                      )}
                      {selectedRequest.status === "APPROVED" && (
                          <div className="rounded-xl bg-[#dff7ec] px-4 py-3 text-sm font-bold text-[#14956c]">
                            관리자 최종 승인이 완료되어 스케줄이 교체되었습니다.
                          </div>
                      )}
                      {selectedRequest.status === "CLOSED" && (
                          <div className="rounded-xl bg-[#f1f3f2] px-4 py-3 text-sm font-bold text-[#66736d]">
                            종료된 요청입니다.
                          </div>
                      )}
                    </div>
                  </div>

                  <div className="mt-6 flex gap-3">
                    <button
                        type="button"
                        onClick={() => setSelectedRequest(null)}
                        disabled={isSubmitting}
                        className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7] disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      닫기
                    </button>

                    {selectedRequest.status === "OPEN" && (
                        <button
                            type="button"
                            onClick={() => handleClose(selectedRequest.requestId)}
                            disabled={isSubmitting}
                            className="flex-1 rounded-xl border border-[#f1cccc] px-4 py-3 text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5] disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          요청 종료
                        </button>
                    )}

                    {selectedRequest.status === "ACCEPTED" && (
                        <button
                            type="button"
                            onClick={() => handleApprove(selectedRequest.requestId)}
                            disabled={isSubmitting}
                            className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52] disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          최종 승인
                        </button>
                    )}
                  </div>
                </div>
              </div>
          );
        })()}
      </>
  );
}