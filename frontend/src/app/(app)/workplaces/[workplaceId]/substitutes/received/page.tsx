"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch } from "@/lib/api";

type RequestStatus = "PENDING" | "ACCEPTED";

type SubstituteRequest = {
  id: number;
  requesterName: string;
  workplaceName: string;
  date: string;
  day: string;
  startTime: string;
  endTime: string;
  status: RequestStatus;
};

type ReceivedSubstituteRequestResponse = {
  candidateId: number;
  requestId: number;
  shiftId: number;
  workplaceId: number;
  workplaceName: string;
  requesterMemberId: number;
  requesterName: string;
  startAt: string;
  endAt: string;
  requestStatus: "OPEN";
  candidateStatus: "PENDING";
  createdAt: string;
};

type AcceptedSubstituteRequestResponse = {
  candidateId: number;
  requestId: number;
  shiftId: number;
  workplaceId: number;
  workplaceName: string;
  requesterName: string;
  startAt: string;
  endAt: string;
  requestStatus: "ACCEPTED";
  candidateStatus: "ACCEPTED";
  respondedAt: string;
};


type Tab = "PENDING" | "ACCEPTED";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];
const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

function parseAsKst(iso: string): Date {
  return new Date(`${iso}+09:00`);
}

function toKstDisplay(iso: string): Date {
  return new Date(parseAsKst(iso).getTime() + KST_OFFSET_MS);
}

function formatDateLabel(iso: string): string {
  const date = toKstDisplay(iso);
  return `${date.getUTCMonth() + 1}월 ${date.getUTCDate()}일`;
}

function formatDayLabel(iso: string): string {
  return WEEKDAY_LABELS[toKstDisplay(iso).getUTCDay()];
}

function formatTimeLabel(iso: string): string {
  const date = toKstDisplay(iso);
  const hour = String(date.getUTCHours()).padStart(2, "0");
  const minute = String(date.getUTCMinutes()).padStart(2, "0");

  return `${hour}:${minute}`;
}

export default function ReceivedRequestsPage() {
  const params = useParams();
  const workplaceId = Number(params.workplaceId);

  const [requests, setRequests] = useState<SubstituteRequest[]>([]);
  const [tab, setTab] = useState<Tab>("PENDING");
  const [isResponding, setIsResponding] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    const fetchRequests = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);

        const [receivedData, acceptedData] = await Promise.all([
          apiFetch<ReceivedSubstituteRequestResponse[]>(
              "/substitute-requests/received"
          ),
          apiFetch<AcceptedSubstituteRequestResponse[]>(
              "/substitute-requests/accepted"
          ),
        ]);

        const receivedRequests: SubstituteRequest[] = receivedData
        .filter((request) => request.workplaceId === workplaceId)
        .map((request) => {

          return {
            id: request.candidateId,
            requesterName: request.requesterName,
            workplaceName: request.workplaceName,
            date: formatDateLabel(request.startAt),
            day: formatDayLabel(request.startAt),
            startTime: formatTimeLabel(request.startAt),
            endTime: formatTimeLabel(request.endAt),
            status: "PENDING",
          };
        });

        const acceptedRequests: SubstituteRequest[] = acceptedData
        .filter((request) => request.workplaceId === workplaceId)
        .map((request) => {

          return {
            id: request.candidateId,
            requesterName: request.requesterName,
            workplaceName: request.workplaceName,
            date: formatDateLabel(request.startAt),
            day: formatDayLabel(request.startAt),
            startTime: formatTimeLabel(request.startAt),
            endTime: formatTimeLabel(request.endAt),
            status: "ACCEPTED",
          };
        });

        if (ignore) return;

        setRequests([...receivedRequests, ...acceptedRequests]);
      } catch (error) {
        if (ignore) return;

        setLoadError(
            error instanceof Error
                ? error.message
                : "대타 요청을 불러오지 못했습니다."
        );
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    };

    fetchRequests();

    return () => {
      ignore = true;
    };
  }, [workplaceId]);

  const handleAccept = async (candidateId: number) => {
    try {
      setIsResponding(true);

      await apiFetch(
          `/substitute-candidates/${candidateId}/response`,
          {
            method: "PATCH",
            body: JSON.stringify({
              decision: "ACCEPT",
            }),
          }
      );

      setRequests((prevRequests) =>
          prevRequests.map((request) =>
              request.id === candidateId
                  ? { ...request, status: "ACCEPTED" }
                  : request
          )
      );
    } catch (error) {
      const message =
          error instanceof Error
              ? error.message
              : "대타 요청 수락에 실패했습니다.";

      alert(message);
    } finally {
      setIsResponding(false);
    }
  };

  const handleReject = async (candidateId: number) => {
    try {
      setIsResponding(true);

      await apiFetch(
          `/substitute-candidates/${candidateId}/response`,
          {
            method: "PATCH",
            body: JSON.stringify({
              decision: "REJECT",
            }),
          }
      );

      setRequests((prevRequests) =>
          prevRequests.filter((request) => request.id !== candidateId)
      );
    } catch (error) {
      const message =
          error instanceof Error
              ? error.message
              : "대타 요청 거절에 실패했습니다.";

      alert(message);
    } finally {
      setIsResponding(false);
    }
  };

  const filteredRequests = requests.filter(
      (request) => request.status === tab
  );

  const pendingCount = requests.filter(
      (request) => request.status === "PENDING"
  ).length;

  const acceptedCount = requests.filter(
      (request) => request.status === "ACCEPTED"
  ).length;

  return (
      <>
        <PageHeader
            title="받은 요청"
            description="나에게 도착한 대체 근무 요청을 확인하고 응답하세요."
        />

        {/* 탭 */}
        <div className="mb-5 flex flex-wrap gap-2">
          <button
              type="button"
              onClick={() => setTab("PENDING")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "PENDING"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            대기 중 {pendingCount}
          </button>

          <button
              type="button"
              onClick={() => setTab("ACCEPTED")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "ACCEPTED"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            승인 대기 {acceptedCount}
          </button>

        </div>

        {/* 요청 목록 */}
        <div className="space-y-4">

          {isLoading && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#dce8e2] bg-white text-sm text-[#78847f]">
                대타 요청을 불러오는 중입니다...
              </div>
          )}

          {!isLoading && loadError && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#f1cccc] bg-white text-sm text-[#d95555]">
                {loadError}
              </div>
          )}

          {!isLoading && !loadError && filteredRequests.map((request) => (
              <div
                  key={request.id}
                  className="rounded-2xl border border-[#dce8e2] bg-white p-5 shadow-sm"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-black">
                      {request.date} ({request.day})
                    </h2>

                    <p className="mt-1 font-bold text-[#005642]">
                      {request.startTime} - {request.endTime}
                    </p>
                  </div>

                  {request.status === "PENDING" && (
                      <span className="rounded-full bg-[#fff1d7] px-3 py-1 text-xs font-bold text-[#a96d09]">
                        대기 중
                      </span>
                  )}

                  {request.status === "ACCEPTED" && (
                      <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                        승인 대기
                      </span>
                  )}
                </div>

                {/* 요청자 */}
                <div className="mt-5 rounded-xl bg-[#f3fbf7] p-4">
                  <p className="text-xs font-semibold text-[#78847f]">
                    요청자
                  </p>

                  <p className="mt-1 font-black">{request.requesterName}</p>

                  <p className="mt-1 text-sm text-[#78847f]">
                    {request.workplaceName}
                  </p>
                </div>

                {/* 대기 중일 때만 버튼 표시 */}
                {request.status === "PENDING" && (
                    <div className="mt-5 grid grid-cols-2 gap-3">
                      <button
                          type="button"
                          onClick={() => handleReject(request.id)}
                          disabled={isResponding}
                          className="rounded-xl border border-[#f1cccc] px-4 py-3 font-bold text-[#d95555] transition hover:bg-[#fff5f5] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {isResponding ? "처리 중..." : "거절하기"}
                      </button>

                      <button
                          type="button"
                          onClick={() => handleAccept(request.id)}
                          disabled={isResponding}
                          className="rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {isResponding ? "처리 중..." : "수락하기"}
                      </button>
                    </div>
                )}

                {request.status === "ACCEPTED" && (
                    <div className="mt-5 rounded-xl bg-[#f3fbf7] px-4 py-3 text-center text-sm font-bold text-[#14956c]">
                      대체 근무 요청을 수락했습니다. 관리자 최종 승인을 기다리고 있습니다.
                    </div>
                )}
              </div>
          ))}

          {/* 비어있는 탭 */}
          {!isLoading && !loadError && filteredRequests.length === 0 && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#dce8e2] bg-white text-sm text-[#78847f]">
                {tab === "PENDING" && "현재 대기 중인 요청이 없습니다."}
                {tab === "ACCEPTED" && "승인 대기 중인 요청이 없습니다."}
              </div>
          )}
        </div>
      </>
  );
}