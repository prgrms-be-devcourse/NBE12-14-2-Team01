"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch, ApiError } from "@/lib/api";

// 목록용 DTO
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

// 상세 조회용 DTO
type ShiftDetailResponse = {
  shiftId: number;
  scheduleId: number;
  workplaceId: number;
  workplaceName: string;
  weekStartDate: string;
  startAt: string;
  endAt: string;
  status: "SCHEDULED" | "CANCELED" | string;
  publishedAt: string | null;
};

// UI 전용 타입
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

// 오늘 날짜 기준 '월요일' YYYY-MM-DD 반환 유틸
function getMondayOfCurrentWeek(d = new Date()): string {
  const date = new Date(d);
  const day = date.getDay();
  // 일요일(0)이면 -6일, 월~토(1~6)이면 1-day 만큼 이동
  const diff = date.getDate() - day + (day === 0 ? -6 : 1);
  date.setDate(diff);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const dayStr = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${dayStr}`;
}

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

// 근무 시간 계산 (자정 넘어가는 밤샘 근무 예외 처리 보장)
function getWorkHours(startTime: string, endTime: string) {
  const [startHour, startMinute] = startTime.split(":").map(Number);
  const [endHour, endMinute] = endTime.split(":").map(Number);

  const startMins = startHour * 60 + startMinute;
  let endMins = endHour * 60 + endMinute;

  // 익일 종료 근무인 경우 (예: 22:00 ~ 06:00)
  if (endMins < startMins) {
    endMins += 24 * 60;
  }

  return (endMins - startMins) / 60;
}

// YYYY-MM-DD 날짜 계산 유틸
function addDays(dateStr: string, days: number): string {
  const date = new Date(dateStr);
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// YYYY-MM-DD -> "YYYY년 M월 N주차" 포맷 변환
function formatWeekTitle(dateStr: string): string {
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const weekNum = Math.ceil(day / 7);
  return `${year}년 ${month}월 ${weekNum}주차`;
}

// 날짜/시간 포맷팅 유틸 (ISO -> YYYY.MM.DD HH:mm)
function formatDateTime(isoString: string | null) {
  if (!isoString) return "-";
  const date = new Date(isoString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}.${month}.${day} ${hours}:${minutes}`;
}

export default function MyShiftsPage() {
  const params = useParams();
  const workplaceId = params.workplaceId as string;

  // 현재 주 월요일 날짜를 기본값으로 설정
  const [currentWeekStart, setCurrentWeekStart] = useState<string>(() =>
      getMondayOfCurrentWeek()
  );
  const [shifts, setShifts] = useState<MyShift[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  // 상세조회 모달 State
  const [selectedShiftDetail, setSelectedShiftDetail] =
      useState<ShiftDetailResponse | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  // 주 이동 핸들러
  const handlePrevWeek = () => {
    setCurrentWeekStart((prev) => addDays(prev, -7));
  };

  const handleNextWeek = () => {
    setCurrentWeekStart((prev) => addDays(prev, 7));
  };

  // 상세 데이터 조회 핸들러
  const handleOpenDetail = async (shiftId: number) => {
    setIsDetailLoading(true);
    setDetailError("");

    try {
      const detailData = await apiFetch<ShiftDetailResponse>(
          `/workplaces/${workplaceId}/shifts/${shiftId}`
      );
      setSelectedShiftDetail(detailData);
    } catch (err) {
      if (err instanceof ApiError) {
        setDetailError(err.message);
      } else if (err instanceof Error) {
        setDetailError(err.message);
      } else {
        setDetailError("근무 상세 정보를 불러오는 중 오류가 발생했습니다.");
      }
    } finally {
      setIsDetailLoading(false);
    }
  };

  const handleCloseDetail = useCallback(() => {
    setSelectedShiftDetail(null);
    setDetailError("");
  }, []);

  // 모달 열림 상태일 때 키보드 ESC 누르면 닫기 & 스크롤 방지
  useEffect(() => {
    const isModalOpen = Boolean(selectedShiftDetail || isDetailLoading || detailError);
    if (!isModalOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") handleCloseDetail();
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = "unset";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [selectedShiftDetail, isDetailLoading, detailError, handleCloseDetail]);

  // 근무 목록 조회 API
  useEffect(() => {
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
                  aria-label="이전 주"
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">‹</span>
              </button>

              <p className="font-bold">{formatWeekTitle(currentWeekStart)}</p>

              <button
                  type="button"
                  onClick={handleNextWeek}
                  aria-label="다음 주"
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
          ) : shifts.length > 0 ? (
              <div className="space-y-3">
                {shifts.map((shift) => (
                    <div
                        key={shift.id}
                        onClick={() => handleOpenDetail(shift.id)}
                        className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-[#dce8e2] p-5 transition hover:border-[#005642] hover:bg-[#fcfdfc]"
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
          ) : (
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

        {/* 상세조회 모달 UI */}
        {(selectedShiftDetail || isDetailLoading || detailError) && (
            <div
                className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
                onClick={handleCloseDetail}
            >
              <div
                  className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
                  onClick={(e) => e.stopPropagation()}
              >
                {isDetailLoading ? (
                    <div className="py-12 text-center text-sm text-[#78847f]">
                      상세 정보를 불러오는 중입니다...
                    </div>
                ) : detailError ? (
                    <div className="space-y-4 py-4 text-center">
                      <p className="text-sm text-[#d95555]">{detailError}</p>
                      <button
                          type="button"
                          onClick={handleCloseDetail}
                          className="rounded-xl bg-[#f3fbf7] px-4 py-2 text-sm font-bold text-[#005642]"
                      >
                        닫기
                      </button>
                    </div>
                ) : selectedShiftDetail ? (
                    <div>
                      <div className="flex items-center justify-between border-b border-[#edf2ef] pb-4">
                        <h3 className="text-lg font-black text-[#005642]">
                          근무 상세 정보
                        </h3>
                        <button
                            type="button"
                            onClick={handleCloseDetail}
                            className="text-gray-400 transition hover:text-gray-600"
                        >
                          ✕
                        </button>
                      </div>

                      <div className="mt-4 space-y-3 text-sm">
                        <div className="flex justify-between">
                          <span className="text-[#78847f]">사업장</span>
                          <span className="font-bold text-[#005642]">
                      {selectedShiftDetail.workplaceName}
                    </span>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-[#78847f]">근무 시작</span>
                          <span className="font-bold">
                      {formatDateTime(selectedShiftDetail.startAt)}
                    </span>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-[#78847f]">근무 종료</span>
                          <span className="font-bold">
                      {formatDateTime(selectedShiftDetail.endAt)}
                    </span>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-[#78847f]">상태</span>
                          <span className="rounded-full bg-[#dff7ec] px-2.5 py-0.5 text-xs font-bold text-[#14956c]">
                      {selectedShiftDetail.status}
                    </span>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-[#78847f]">주 시작일</span>
                          <span className="font-medium">
                      {selectedShiftDetail.weekStartDate}
                    </span>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-[#78847f]">게시 일시</span>
                          <span className="font-medium">
                      {formatDateTime(selectedShiftDetail.publishedAt)}
                    </span>
                        </div>

                        <div className="flex justify-between border-t border-[#edf2ef] pt-2 text-xs text-gray-400">
                          <span>Shift ID: {selectedShiftDetail.shiftId}</span>
                          <span>Schedule ID: {selectedShiftDetail.scheduleId}</span>
                        </div>
                      </div>

                      <button
                          type="button"
                          onClick={handleCloseDetail}
                          className="mt-6 w-full rounded-xl bg-[#005642] py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                      >
                        확인
                      </button>
                    </div>
                ) : null}
              </div>
            </div>
        )}
      </>
  );
}