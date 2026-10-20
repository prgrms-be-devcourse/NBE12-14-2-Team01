"use client";

import {useEffect, useState, useCallback, useRef} from "react";
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
  workplaceId: number;
  workplaceName: string;
  date: string;
  day: string;
  dayNumber: string;
  startTime: string;
  endTime: string;
  startAt: string;
  endAt: string;
  status: "SCHEDULED";
};

// 서버의 LocalDateTime 문자열을 무조건 KST(UTC+9) Date 객체로 해석
function parseAsKst(isoString: string | null): Date | null {
  if (!isoString) return null;
  // 타임존 지정(Z 또는 +09:00 등)이 없는 경우 +09:00을 붙여 KST로 강제 고정
  const hasTimeZone = /[Z+-]\d{2}:?\d{2}$/.test(isoString);
  const kstFormattedString = hasTimeZone ? isoString : `${isoString}+09:00`;
  return new Date(kstFormattedString);
}

// KST 기준 날짜 변환 유틸리티
function getKstDate(date = new Date()): Date {
  const utc = date.getTime() + date.getTimezoneOffset() * 60000;
  return new Date(utc + 9 * 60 * 60 * 1000);
}

// 오늘 날짜 기준 '월요일' YYYY-MM-DD 반환 유틸
function getMondayOfCurrentWeek(): string {
  const date = getKstDate();
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
  const startDate = parseAsKst(dto.startAt);
  const endDate = parseAsKst(dto.endAt);

  if (!startDate || !endDate) {
    throw new Error("Invalid date string");
  }

  const formatKstTime = (date: Date) =>
      date.toLocaleTimeString("ko-KR", {
        timeZone: "Asia/Seoul",
        hour12: false,
        hour: "2-digit",
        minute: "2-digit",
      });

  // formatToParts()로 월, 일 숫자만 안전하게 추출
  const formatter = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    month: "numeric",
    day: "numeric",
    weekday: "short",
  });

  const parts = formatter.formatToParts(startDate);
  const month = parts.find((p) => p.type === "month")?.value || "";
  const dayNum = parts.find((p) => p.type === "day")?.value || "";
  const dayName = parts.find((p) => p.type === "weekday")?.value || "";

  return {
    id: dto.shiftId,
    workplaceId: dto.workplaceId,
    workplaceName: dto.workplaceName,
    date: `${month}월 ${dayNum}일`,
    day: dayName,
    dayNumber: dayNum,
    startTime: formatKstTime(startDate),
    endTime: formatKstTime(endDate),
    startAt: dto.startAt,
    endAt: dto.endAt,
    status: dto.status,
  };
}

type ShiftDisplayStatus = "예정" | "근무 중" | "완료";

function getShiftDisplayStatus(
    startAt: string,
    endAt: string
): ShiftDisplayStatus {
  const now = new Date();
  const start = parseAsKst(startAt);
  const end = parseAsKst(endAt);

  if (!start || !end) {
    return "예정";
  }

  if (now < start) {
    return "예정";
  }

  if (now < end) {
    return "근무 중";
  }

  return "완료";
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
  const [year, month, day] = dateStr.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  date.setUTCDate(date.getUTCDate() + days);

  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, "0");
  const d = String(date.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// YYYY-MM-DD -> "YYYY년 M월 N주차" 포맷 변환
function formatWeekTitle(dateStr: string): string {
  const [year, month, day] = dateStr.split("-").map(Number);

  // 1일의 요일 구하기 (0: 일, 1: 월, ..., 6: 토)
  const firstDay = new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
  const offset = firstDay === 0 ? 6 : firstDay - 1; // 월요일 시작 기준 오프셋
  const weekNum = Math.ceil((day + offset) / 7);

  return `${year}년 ${month}월 ${weekNum}주차`;
}

// 날짜/시간 포맷팅 유틸 (ISO -> KST YYYY.MM.DD HH:mm)
function formatDateTime(isoString: string | null) {
  if (!isoString) return "-";
  const date =parseAsKst(isoString);
  if (!date) return "-";
  const formatted = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);

  return formatted.replace(/\. /g, ".").replace(/\.$/, "");
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
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedShiftDetail, setSelectedShiftDetail] =
      useState<ShiftDetailResponse | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  // 최신 상세조회 요청 추적 Ref
  const detailRequestIdRef = useRef(0);

  // 주 이동 핸들러
  const handlePrevWeek = () => {
    setCurrentWeekStart((prev) => addDays(prev, -7));
  };

  const handleNextWeek = () => {
    setCurrentWeekStart((prev) => addDays(prev, 7));
  };

  // 상세 데이터 조회 핸들러 (API: GET /shifts/{shiftId})
  const handleOpenDetail = async (shiftId: number) => {
    // 요청 우선순위를 매겨주는 순서 변수
    const requestId = ++detailRequestIdRef.current;

    setIsModalOpen(true);
    setSelectedShiftDetail(null);
    setIsDetailLoading(true);
    setDetailError("");

    try {
      const detailData = await apiFetch<ShiftDetailResponse>(
          `/shifts/${shiftId}`
      );
      if (requestId === detailRequestIdRef.current) {
        setSelectedShiftDetail(detailData);
      }
    } catch (err) {
      if (requestId === detailRequestIdRef.current) {
        if (err instanceof ApiError || err instanceof Error) {
          setDetailError(err.message);
        } else {
          setDetailError("근무 상세 정보를 불러오는 중 오류가 발생했습니다.");
        }
      }
    } finally {
      if (requestId === detailRequestIdRef.current) {
        setIsDetailLoading(false);
      }
    }
  };

  const handleCloseDetail = useCallback(() => {
    detailRequestIdRef.current += 1; // 진행 중인 요청 무시
    setIsModalOpen(false);
    setSelectedShiftDetail(null);
    setIsDetailLoading(false);
    setDetailError("");
  }, []);

  // 모달 열림 상태일 때 키보드 ESC 누르면 닫기 & 스크롤 방지
  useEffect(() => {
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
  }, [isModalOpen, handleCloseDetail]);

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
          //서버에서 잘못된 날짜 형식을 보낸다면 전체가 튕기는 게 아닌 정상 데이터만 필터링해 표시
          const formattedShifts = response.shifts
          .map((dto) => {
            try {
              return parseShiftDto(dto);
            } catch {
              return null; // 파싱 실패한 항목은 null 처리
            }
          })
          .filter((shift): shift is MyShift => shift !== null); // null 항목 제거

          setShifts(formattedShifts);
        }
      } catch (err) {
        if (!isCancelled) {
          if (err instanceof ApiError || err instanceof Error) {
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
                          <div className="flex items-center gap-2">
                            <p className="font-black">
                              {shift.date} ({shift.day})
                            </p>
                            {/* 근무지 구분 표시 */}
                            <span className="rounded bg-[#edf2ef] px-2 py-0.5 text-xs font-bold text-[#4a5568]">
                        {shift.workplaceName}
                      </span>
                          </div>

                          <p className="mt-1 text-sm text-[#78847f]">
                            {shift.startTime} ~ {shift.endTime}
                          </p>
                        </div>
                      </div>

                      <span className="shrink-0 rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                        {getShiftDisplayStatus(shift.startAt, shift.endAt)}
                      </span>
                    </div>
                ))}
              </div>
          ) : (
              <div className="flex min-h-[180px] items-center justify-center rounded-xl border border-dashed border-[#dce8e2] text-sm text-[#78847f]">
                이번 주 근무가 없습니다.
              </div>
          )}
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">이번 주 요약</h2>

            <p className="mt-1 text-sm text-[#78847f]">
              이번 주 근무를 간단히 확인하세요.
            </p>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">총 근무</p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {isLoading || error ? "-" : `${shifts.length}회`}
              </p>
            </div>

            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">이번 주 근무 시간</p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {isLoading || error ? "-" : `${totalHours}시간`}
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
        {isModalOpen && (
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
                            {getShiftDisplayStatus(
                                selectedShiftDetail.startAt,
                                selectedShiftDetail.endAt
                            )}
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