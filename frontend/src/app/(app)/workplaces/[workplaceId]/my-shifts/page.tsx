"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch, ApiError } from "@/lib/api";

// DTO용 type
type ShiftItemDto = {
  shiftId: number;
  scheduleId: number;
  workplaceId: number;
  workplaceName: string;
  startAt: string;
  endAt: string;
  status: "SCHEDULED";
};

type ShiftListResponse = {
  weekStartDate: string;
  shifts: ShiftItemDto[];
};

type MyShift = {
  id: number;
  date: string;
  day: string;
  dayNumber: string;
  startTime: string;
  endTime: string;
  status: "SCHEDULED";
};

// 요일 변환용 배열
const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

function parseShiftDto(dto: ShiftItemDto): MyShift {
  const startDate = new Date(dto.startAt);
  const endDate = new Date(dto.endAt);

  const month = startDate.getMonth() + 1;
  const dayNum = startDate.getDate();
  const dayName = DAY_NAMES[startDate.getDay()];

  const formatTime = (date: Date) =>
      `${String(date.getHours()).padStart(2, "0")}:${String(
          date.getMinutes()
      ).padStart(2, "0")}`;

  return {
    id: dto.shiftId,
    date: `${month}월 ${dayNum}일`,
    day: dayName,
    dayNumber: String(dayNum),
    startTime: formatTime(startDate),
    endTime: formatTime(endDate),
    status: dto.status,
  };
}

function getWorkHours(startTime: string, endTime: string) {
  const [startHour, startMinute] = startTime.split(":").map(Number);
  const [endHour, endMinute] = endTime.split(":").map(Number);

  const start = startHour * 60 + startMinute;
  const end = endHour * 60 + endMinute;

  return (end - start) / 60;
}
// YYYY-MM-DD 날짜에 days(일수)를 더하거나 빼주는 유틸 함수
function addDays(dateStr: string, days: number): string {
  const date = new Date(dateStr);
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// YYYY-MM-DD 포맷을 "YYYY년 M월 N주차" 텍스트로 전환해 주는 함수
function formatWeekTitle(dateStr: string): string {
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const weekNum = Math.ceil(day / 7);
  return `${year}년 ${month}월 ${weekNum}주차`;
}

export default function MyShiftsPage() {
  const params = useParams();
  const workplaceId = params.workplaceId as string;

  // 현재 조회 기준이 되는 월요일 날짜 (기본값: 2026-09-21)
  const [currentWeekStart, setCurrentWeekStart] = useState<string>("2026-09-21");
  const [shifts, setShifts] = useState<MyShift[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  // 이전 주 / 다음 주 이동 핸들러
  const handlePrevWeek = () => {
    const prevWeek = addDays(currentWeekStart, -7);
    setIsLoading(true);
    setCurrentWeekStart(prevWeek);
  };

  const handleNextWeek = () => {
    const nextWeek = addDays(currentWeekStart, 7);
    setIsLoading(true);
    setCurrentWeekStart(nextWeek);
  };

  useEffect(() => {
    // SSR 환경(Node.js 서버 렌더링 시점)에서는 실행 방지
    if (typeof window === "undefined") return;

    let isCancelled = false;

    const loadMyShifts = async () => {
      setIsLoading(true);
      setError("");

      try {
        const response = await apiFetch<ShiftListResponse>(
            `/schedules/me?weekStartDate=${currentWeekStart}`
        );

        if (!isCancelled) {
          const formattedShifts = response.shifts.map(parseShiftDto);
          setShifts(formattedShifts);
        }
      } catch (err) {
        if (!isCancelled) {
          if (err instanceof ApiError) {
            setError(err.message);
          } else if (err instanceof Error) {
            setError(err.message);
          } else {
            setError("근무 일정을 불러오는 중 오류가 발생했습니다.");
          }
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadMyShifts();

    return () => {
      isCancelled = true;
    };
  }, [currentWeekStart]);

  const totalHours = shifts.reduce(
      (total, shift) => total + getWorkHours(shift.startTime, shift.endTime),
      0
  );

  return (
      <>
        <PageHeader
            title="내 근무"
            description="이번 주 나의 근무 일정을 확인하세요."
        />

        <Card>
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <button
                  type="button"
                  onClick={handlePrevWeek}
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">‹</span>
              </button>

              <p className="font-bold">{formatWeekTitle(currentWeekStart)}</p>

              <button
                  type="button"
                  onClick={handleNextWeek}
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">›</span>
              </button>
            </div>

            <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
            공개됨
          </span>
          </div>

          {/* 로딩/에러/리스트 조건부 렌더링 */}
          {isLoading ? (
              <div className="flex min-h-[180px] items-center justify-center text-sm text-[#78847f]">
                근무 정보를 불러오는 중입니다...
              </div>
          ) : error ? (
              <div className="flex min-h-[180px] items-center justify-center text-sm text-[#d95555]">
                {error}
              </div>
          ) : (
              <div className="space-y-3">
                {shifts.map((shift) => (
                    <div
                        key={shift.id}
                        className="flex items-center justify-between gap-4 rounded-xl border border-[#dce8e2] p-5"
                    >
                      <div className="flex items-center gap-4">
                        <div className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-xl bg-[#f3fbf7]">
                    <span className="text-xs font-semibold text-[#66736d]">
                      {shift.day}
                    </span>

                          <span className="text-lg font-black text-[#005642]">
                      {shift.dayNumber}일
                    </span>
                        </div>

                        <div>
                          <p className="font-black">
                            {shift.date} ({shift.day})
                          </p>

                          <p className="mt-1 text-sm text-[#78847f]">
                            {shift.startTime} ~ {shift.endTime}
                          </p>
                        </div>
                      </div>

                      <span className="shrink-0 rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                  예정
                </span>
                    </div>
                ))}
              </div>
          )}

          {!isLoading && !error && shifts.length === 0 && (
              <div className="flex min-h-[180px] items-center justify-center rounded-xl border border-dashed border-[#dce8e2] text-sm text-[#78847f]">
                이번 주 예정된 근무가 없습니다.
              </div>
          )}
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">이번 주 요약</h2>

            <p className="mt-1 text-sm text-[#78847f]">
              예정된 근무를 간단히 확인하세요.
            </p>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">총 근무</p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {shifts.length}회
              </p>
            </div>

            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">예정 시간</p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {totalHours}시간
              </p>
            </div>
          </div>

          <div className="mt-6 border-t border-[#edf2ef] pt-6">
            <p className="font-bold">근무가 어려우신가요?</p>

            <p className="mt-1 text-sm text-[#78847f]">
              예정된 근무 중 참여하기 어려운 일정이 있다면 대체 근무를 요청할 수
              있어요.
            </p>

            <Link
                href={`/workplaces/${workplaceId}/substitutes/request`}
                className="mt-4 block w-full rounded-xl bg-[#005642] px-4 py-3 text-center text-sm font-bold text-white transition hover:bg-[#0b6b52]"
            >
              대체 근무 요청하기
            </Link>
          </div>
        </Card>
      </>
  );
}