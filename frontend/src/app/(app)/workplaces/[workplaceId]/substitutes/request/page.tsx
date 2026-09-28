"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import { ApiError, apiFetch, UnexpectedResponseError } from "@/lib/api";

// 내 근무 목록 응답
type ShiftItem = {
  shiftId: number;
  scheduleId: number;
  workplaceId: number;
  workplaceName: string;
  startAt: string;
  endAt: string;
  status: string;
};

type ShiftListResponse = {
  weekStartDate: string;
  shifts: ShiftItem[];
};

// 대타 요청 보낸 뒤 응답
type SubstituteRequestCreateResponse = {
  requestCreated: boolean;
  requestId: number | null;
  shiftId: number;
  status: string | null;
  candidateCount: number;
};

// 내가 보낸 요청 목록 응답 (필요한 것만)
type SentSubstituteRequest = {
  shiftId: number;
  status: string;
};

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];
const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

// 지금 한국 시간
function nowInKst(): Date {
  return new Date(Date.now() + KST_OFFSET_MS);
}

// 서버 시간 문자열을 한국 시간으로 읽음
function parseAsKst(iso: string): Date {
  return new Date(`${iso}+09:00`);
}

// 이번 주 월요일 날짜 구하기 (서버가 월요일만 받음)
function getThisMondayDateString(): string {
  const today = nowInKst();
  const day = today.getUTCDay(); // 0(일) ~ 6(토)
  const diffToMonday = day === 0 ? -6 : 1 - day;

  const monday = new Date(today);
  monday.setUTCDate(today.getUTCDate() + diffToMonday);

  const year = monday.getUTCFullYear();
  const month = String(monday.getUTCMonth() + 1).padStart(2, "0");
  const date = String(monday.getUTCDate()).padStart(2, "0");

  return `${year}-${month}-${date}`;
}

function toKstDisplay(iso: string): Date {
  return new Date(parseAsKst(iso).getTime() + KST_OFFSET_MS);
}

function formatDateLabel(iso: string): string {
  const d = toKstDisplay(iso);
  return `${d.getUTCMonth() + 1}월 ${d.getUTCDate()}일`;
}

function formatDayLabel(iso: string): string {
  return WEEKDAY_LABELS[toKstDisplay(iso).getUTCDay()];
}

function formatTimeLabel(iso: string): string {
  const d = toKstDisplay(iso);
  const hh = String(d.getUTCHours()).padStart(2, "0");
  const mm = String(d.getUTCMinutes()).padStart(2, "0");
  return `${hh}:${mm}`;
}

export default function SubstituteRequestPage() {
  const params = useParams();
  const workplaceId = Number(params.workplaceId);

  // 이번 주 내 근무 목록, null이면 불러오는 중
  const [shifts, setShifts] = useState<ShiftItem[] | null>(null);
  const [listError, setListError] = useState("");
  // 이미 요청 보낸 근무 id 목록
  const [requestedShiftIds, setRequestedShiftIds] = useState<number[]>([]);

  const [selectedShiftId, setSelectedShiftId] = useState<number | null>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [noCandidateMessage, setNoCandidateMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    async function loadShifts() {
      try {
        const weekStartDate = getThisMondayDateString();
        const [data, sentRequests] = await Promise.all([
          apiFetch<ShiftListResponse>(
              `/schedules/me?weekStartDate=${weekStartDate}`
          ),
          apiFetch<SentSubstituteRequest[]>("/substitute-requests/sent"),
        ]);

        // 지금 근무지 + 아직 시작 안 한 근무만 남김
        const now = new Date();
        const upcoming = data.shifts.filter(
            (shift) =>
                shift.workplaceId === workplaceId &&
                parseAsKst(shift.startAt) > now
        );

        // 진행 중인 요청이 있는 근무는 (요청 보냄) 표시하려고 따로 모아둠
        const requested = sentRequests
            .filter(
                (request) =>
                    request.status === "OPEN" || request.status === "ACCEPTED"
            )
            .map((request) => request.shiftId);

        setShifts(upcoming);
        setRequestedShiftIds(requested);
        setSelectedShiftId(
            upcoming.find((shift) => !requested.includes(shift.shiftId))
                ?.shiftId ?? null
        );
      } catch (fetchError) {
        if (fetchError instanceof ApiError) {
          setListError(fetchError.message);
        } else if (
            fetchError instanceof UnexpectedResponseError ||
            fetchError instanceof TypeError
        ) {
          setListError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
        } else {
          console.error(fetchError);
          setListError("근무 목록을 불러오는 중 문제가 발생했습니다.");
        }

        setShifts([]);
      }
    }

    loadShifts();
  }, [workplaceId]);

  const selectedShift = shifts?.find(
      (shift) => shift.shiftId === selectedShiftId
  );

  const resetMessages = () => {
    setError("");
    setNoCandidateMessage("");
    setSuccessMessage("");
  };

  const handleSubmit = async () => {
    if (!selectedShift) {
      setError("대체 근무를 요청할 근무를 선택해주세요.");
      return;
    }

    resetMessages();
    setIsSubmitting(true);

    try {
      const data = await apiFetch<SubstituteRequestCreateResponse>(
          `/shifts/${selectedShift.shiftId}/substitute-requests`,
          { method: "POST" }
      );

      if (data.requestCreated) {
        setSuccessMessage(
            `대체 근무 요청이 전송되었습니다. (후보 ${data.candidateCount}명에게 전달됨)`
        );

        // 방금 보낸 근무는 (요청 보냄) 처리하고 다음 근무 선택
        const nextRequested = [...requestedShiftIds, selectedShift.shiftId];
        setRequestedShiftIds(nextRequested);
        setSelectedShiftId(
            (shifts ?? []).find((shift) => !nextRequested.includes(shift.shiftId))
                ?.shiftId ?? null
        );
      } else {
        // 후보가 없는 경우, 에러는 아님
        setNoCandidateMessage("현재 조건에 맞는 대체 근무 후보가 없어요.");
      }
    } catch (submitError) {
      if (submitError instanceof ApiError) {
        setError(submitError.message);
      } else if (
          submitError instanceof UnexpectedResponseError ||
          submitError instanceof TypeError
      ) {
        setError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
      } else {
        console.error(submitError);
        setError("요청 처리 중 문제가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
      <>
        <PageHeader
            title="대체 근무 요청"
            description="참여하기 어려운 근무를 선택하고 대체 근무자를 모집하세요."
        />

        <Card>
          <div>
            <h2 className="text-lg font-black">
              요청 정보
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              대체가 필요한 근무를 선택해주세요.
            </p>
          </div>

          {shifts === null && (
              <p className="mt-6 text-sm text-[#78847f]">
                근무 목록을 불러오는 중이에요...
              </p>
          )}

          {shifts !== null && listError && (
              <p className="mt-6 text-sm font-semibold text-[#d95555]">
                {listError}
              </p>
          )}

          {shifts !== null && !listError && shifts.length === 0 && (
              <div className="mt-6 flex min-h-[120px] items-center justify-center rounded-xl border border-dashed border-[#dce8e2] text-sm text-[#78847f]">
                이번 주엔 요청 가능한 근무가 없어요.
              </div>
          )}

          {shifts !== null && !listError && shifts.length > 0 && (
              <>
                <div className="mt-6">
                  <label className="mb-2 block text-sm font-bold">
                    대체가 필요한 근무
                  </label>

                  <div className="relative">
                    <select
                        value={selectedShiftId ?? ""}
                        disabled={isSubmitting}
                        onChange={(e) => {
                          setSelectedShiftId(Number(e.target.value));
                          resetMessages();
                        }}
                        className="w-full appearance-none rounded-xl border border-[#dce8e2] bg-white px-4 py-3 pr-12 outline-none focus:border-[#14956c]"
                    >
                      {selectedShiftId === null && (
                          <option value="" disabled>
                            모든 근무에 이미 요청을 보냈어요
                          </option>
                      )}

                      {shifts.map((shift) => {
                        const isRequested = requestedShiftIds.includes(shift.shiftId);

                        return (
                            <option
                                key={shift.shiftId}
                                value={shift.shiftId}
                                disabled={isRequested}
                            >
                              {formatDateLabel(shift.startAt)} ({formatDayLabel(shift.startAt)}){" "}
                              {formatTimeLabel(shift.startAt)} - {formatTimeLabel(shift.endAt)} /{" "}
                              {shift.workplaceName}
                              {isRequested ? " (요청 보냄)" : ""}
                            </option>
                        );
                      })}
                    </select>

                    <svg
                        viewBox="0 0 20 20"
                        fill="none"
                        className="pointer-events-none absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#66736d]"
                    >
                      <path
                          d="M6 8L10 12L14 8"
                          stroke="currentColor"
                          strokeWidth="1.8"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                </div>

                {selectedShift && (
                    <div className="mt-5 rounded-xl border border-[#dce8e2] bg-[#fafdfb] p-4">
                      <p className="text-xs font-semibold text-[#78847f]">
                        선택한 근무
                      </p>

                      <p className="mt-2 font-black">
                        {formatDateLabel(selectedShift.startAt)} ({formatDayLabel(selectedShift.startAt)})
                      </p>

                      <p className="mt-1 font-bold text-[#005642]">
                        {formatTimeLabel(selectedShift.startAt)} ~ {formatTimeLabel(selectedShift.endAt)}
                      </p>

                      <p className="mt-1 text-sm text-[#78847f]">
                        {selectedShift.workplaceName}
                      </p>
                    </div>
                )}

                <div className="mt-5 rounded-xl bg-[#f3fbf7] p-4">
                  <p className="text-sm font-black text-[#005642]">
                    요청 전 확인해주세요
                  </p>

                  <div className="mt-2 space-y-1 text-sm leading-6 text-[#66736d]">
                    <p>
                      대체 근무가 가능한 후보는 시스템이 자동으로 확인합니다.
                    </p>

                    <p>
                      요청자는 후보 직원을 직접 선택하거나 개별적으로 연락할 필요가 없습니다.
                    </p>

                    <p>
                      후보 직원이 요청을 수락한 뒤 관리자 최종 승인이 완료되어야 실제 근무자가
                      변경됩니다.
                    </p>
                  </div>
                </div>

                {error && (
                    <p className="mt-4 text-sm font-semibold text-[#d95555]">
                      {error}
                    </p>
                )}

                {noCandidateMessage && (
                    <div className="mt-4 rounded-xl bg-[#f3f5f4] px-4 py-3 text-sm font-bold text-[#66736d]">
                      {noCandidateMessage}
                    </div>
                )}

                {successMessage && (
                    <div
                        className="mt-4 rounded-xl bg-[#dff7ec] px-4 py-3 text-sm font-bold text-[#14956c]">
                      {successMessage}
                    </div>
                )}

                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isSubmitting || !selectedShift}
                    className="mt-5 w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? "요청 보내는 중..." : "대체 근무 요청 보내기"}
                </button>
              </>
          )}
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">
              요청은 어떻게 진행되나요?
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              별도의 연락 없이 시스템 안에서 대체 근무 요청을 진행할 수 있어요.
            </p>
          </div>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <div className="rounded-xl bg-[#f3fbf7] p-4">
            <span className="text-xs font-bold text-[#14956c]">
              01
            </span>

              <p className="mt-2 font-black">
                근무 선택
              </p>

              <p className="mt-1 text-sm leading-6 text-[#78847f]">
                참여하기 어려운 내 공식 근무를 선택합니다.
              </p>
            </div>

            <div className="rounded-xl bg-[#f3fbf7] p-4">
            <span className="text-xs font-bold text-[#14956c]">
              02
            </span>

              <p className="mt-2 font-black">
                후보 자동 모집
              </p>

              <p className="mt-1 text-sm leading-6 text-[#78847f]">
                시스템이 일정이 겹치지 않는 직원을 찾아 요청을 전달합니다.
              </p>
            </div>

            <div className="rounded-xl bg-[#f3fbf7] p-4">
            <span className="text-xs font-bold text-[#14956c]">
              03
            </span>

              <p className="mt-2 font-black">
                관리자 승인
              </p>

              <p className="mt-1 text-sm leading-6 text-[#78847f]">
                후보가 수락하면 관리자 승인 후 최종 근무자가 변경됩니다.
              </p>
            </div>
          </div>
        </Card>
      </>
  );
}
