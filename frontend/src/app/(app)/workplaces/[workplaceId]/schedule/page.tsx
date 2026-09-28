"use client";

import { useState } from "react";
import {useParams} from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch, ApiError } from "@/lib/api";

type ScheduleStatus = "NONE" | "DRAFT" | "PUBLISHED";

type DayKey =
    | "mon"
    | "tue"
    | "wed"
    | "thu"
    | "fri"
    | "sat"
    | "sun";

type Shift = {
  id: number;
  memberId: number;
  memberName: string;
  day: DayKey;
  startTime: string;
  endTime: string;
};

type PositionedShift = Shift & {
  lane: number;
  laneCount: number;
};

// TODO: API 연동 후 실제 주차의 날짜로 대체 (date는 백엔드에 startAt/endAt을 만들 때 필요)
const days: { key: DayKey; label: string; date: string }[] = [
  { key: "mon", label: "9/14 월", date: "2026-09-14" },
  { key: "tue", label: "9/15 화", date: "2026-09-15" },
  { key: "wed", label: "9/16 수", date: "2026-09-16" },
  { key: "thu", label: "9/17 목", date: "2026-09-17" },
  { key: "fri", label: "9/18 금", date: "2026-09-18" },
  { key: "sat", label: "9/19 토", date: "2026-09-19" },
  { key: "sun", label: "9/20 일", date: "2026-09-20" },
];

// TODO: API 연동 후 GET 근무지 구성원 목록으로 대체 (id는 WorkplaceMember의 id)
const members: { id: number; name: string }[] = [
  { id: 1, name: "김지연" },
  { id: 2, name: "이서연" },
  { id: 3, name: "박민수" },
  { id: 4, name: "최하은" },
];

// TODO: API 연동 후 SCH-01 응답으로 대체
// 현재는 정기 근무를 기준으로 주간 근무표가 생성됐다고 가정한 mock 데이터
const generatedShifts: Shift[] = [
  {
    id: 1,
    memberId: 3,
    memberName: "박민수",
    day: "mon",
    startTime: "14:00",
    endTime: "22:00",
  },
  {
    id: 2,
    memberId: 1,
    memberName: "김지연",
    day: "tue",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 3,
    memberId: 1,
    memberName: "김지연",
    day: "wed",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 4,
    memberId: 2,
    memberName: "이서연",
    day: "wed",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 5,
    memberId: 3,
    memberName: "박민수",
    day: "thu",
    startTime: "14:00",
    endTime: "22:00",
  },
  {
    id: 6,
    memberId: 4,
    memberName: "최하은",
    day: "sat",
    startTime: "09:00",
    endTime: "15:00",
  },
];

const START_HOUR = 9;
const END_HOUR = 22;
const HOUR_HEIGHT = 56;

const timeLabels = Array.from(
    { length: END_HOUR - START_HOUR + 1 },
    (_, index) => {
      const hour = START_HOUR + index;
      return `${String(hour).padStart(2, "0")}:00`;
    }
);

function timeToMinutes(time: string) {
  const [hour, minute] = time.split(":").map(Number);
  return hour * 60 + minute;
}

// 같은 시간대에 겹치는 근무를 좌우로 나누기 위한 함수
function getPositionedShifts(shifts: Shift[]): PositionedShift[] {
  const sorted = [...shifts].sort(
      (a, b) => timeToMinutes(a.startTime) - timeToMinutes(b.startTime)
  );

  const result: PositionedShift[] = [];

  let currentGroup: Shift[] = [];
  let groupEnd = -1;

  const processGroup = (group: Shift[]) => {
    if (group.length === 0) {
      return;
    }

    const laneEnds: number[] = [];
    const positioned: { shift: Shift; lane: number }[] = [];

    group.forEach((shift) => {
      const start = timeToMinutes(shift.startTime);
      const end = timeToMinutes(shift.endTime);

      let lane = laneEnds.findIndex((laneEnd) => laneEnd <= start);

      if (lane === -1) {
        lane = laneEnds.length;
        laneEnds.push(end);
      } else {
        laneEnds[lane] = end;
      }

      positioned.push({ shift, lane });
    });

    const laneCount = laneEnds.length;

    positioned.forEach(({ shift, lane }) => {
      result.push({
        ...shift,
        lane,
        laneCount,
      });
    });
  };

  sorted.forEach((shift) => {
    const start = timeToMinutes(shift.startTime);
    const end = timeToMinutes(shift.endTime);

    if (currentGroup.length > 0 && start >= groupEnd) {
      processGroup(currentGroup);
      currentGroup = [];
      groupEnd = -1;
    }

    currentGroup.push(shift);
    groupEnd = Math.max(groupEnd, end);
  });

  processGroup(currentGroup);

  return result;
}

function getShiftStyle(shift: PositionedShift) {
  const startMinutes = timeToMinutes(shift.startTime);
  const endMinutes = timeToMinutes(shift.endTime);
  const scheduleStartMinutes = START_HOUR * 60;

  const top =
      ((startMinutes - scheduleStartMinutes) / 60) * HOUR_HEIGHT;

  const height = ((endMinutes - startMinutes) / 60) * HOUR_HEIGHT;

  return {
    top: `${top}px`,
    height: `${height}px`,
    left: `calc(${(shift.lane / shift.laneCount) * 100}% + 4px)`,
    width: `calc(${100 / shift.laneCount}% - 8px)`,
  };
}

// 에러 코드 상수 정의
const UNAVAILABLE_TIME_CONFLICT_CODE = "SFT-015";

// API 요청 함수 정의
async function createShift(
    workplaceId: string,
    scheduleId: number,
    payload: {
      memberId: number;
      startAt: string;
      endAt: string;
      confirmUnavailableConflict: boolean;
    }
) {
  return await apiFetch<{ shiftId: number; memberName: string; }>(
      `/workplaces/${workplaceId}/schedules/${scheduleId}/shifts`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

// 근무 수정 API (PUT)
async function updateShift(
    workplaceId: string,
    scheduleId: number,
    shiftId: number,
    payload: {
      memberId: number;
      startAt: string;
      endAt: string;
      confirmUnavailableConflict: boolean;
    }
) {
  return await apiFetch<{ shiftId: number; memberName: string }>(
      `/workplaces/${workplaceId}/schedules/${scheduleId}/shifts/${shiftId}`,
      {
        method: "PATCH",
        body: JSON.stringify(payload),
      }
  );
}

// 근무 삭제 API (DELETE)
async function deleteShift(
    workplaceId: string,
    scheduleId: number,
    shiftId: number
) {
  return await apiFetch(
      `/workplaces/${workplaceId}/schedules/${scheduleId}/shifts/${shiftId}`,
      {
        method: "DELETE",
      }
  );
}

export default function SchedulePage() {
  const params = useParams<{ workplaceId: string }>();
  const workplaceId = params.workplaceId;
  const [status, setStatus] = useState<ScheduleStatus>("NONE");
  const [shifts, setShifts] = useState<Shift[]>([]);

  // TODO: SCH-01(이번 주 근무표 생성) API 연동 후, 응답으로 받은 실제 scheduleId로 대체
  const [scheduleId, setScheduleId] = useState<number | null>(null);
  const [showShiftModal, setShowShiftModal] = useState(false);
  const [editingShiftId, setEditingShiftId] = useState<number | null>(null);
  const [selectedMemberId, setSelectedMemberId] = useState<number>(members[0].id);
  const [selectedDay, setSelectedDay] = useState<DayKey>("mon");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("18:00");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // TODO: API 연동 후 SCH-01 호출로 변경 (응답의 schedule.id를 scheduleId로 저장)
  const handleCreateSchedule = () => {
    setShifts(generatedShifts);
    setStatus("DRAFT");
    setScheduleId(1); // 임시값 - 실제 SCH-01 응답의 scheduleId로 교체 필요
  };

  /*
   * TODO: 개발 편의를 위한 임시 기능
   * 기획상 공개 후 미공개 전환은 지원하지 않는다.
   * 상태별 UI 확인을 위해 임시 구현했으며 API 연동 시
   * PUBLISHED -> DRAFT 전환 기능 및 버튼은 제거한다.
   */
  const handleTogglePublish = () => {
    if (status === "DRAFT") {
      setStatus("PUBLISHED");
      return;
    }

    if (status === "PUBLISHED") {
      setStatus("DRAFT");
    }
  };

  const openAddShiftModal = () => {
    setEditingShiftId(null);
    setSelectedMemberId(members[0].id);
    setSelectedDay("mon");
    setStartTime("09:00");
    setEndTime("18:00");
    setError("");
    setShowShiftModal(true);
  };

  const openEditShiftModal = (shift: Shift) => {
    if (status !== "DRAFT") {
      return;
    }
    setEditingShiftId(shift.id);
    setSelectedMemberId(shift.memberId);
    setSelectedDay(shift.day);
    setStartTime(shift.startTime);
    setEndTime(shift.endTime);
    setError("");
    setShowShiftModal(true);
  };

  const closeShiftModal = () => {
    if (isSubmitting) {
      return;
    }
    setShowShiftModal(false);
    setEditingShiftId(null);
    setError("");
  };

  const closeAfterSuccess = () => {
    setShowShiftModal(false);
    setEditingShiftId(null);
    setError("");
  };

  const submitCreateShift = async (confirmUnavailableConflict: boolean) => {
    if (scheduleId === null) {
      setError("근무표가 아직 생성되지 않았습니다.");
      return;
    }

    const day = days.find((d) => d.key === selectedDay);
    const member = members.find((m) => m.id === selectedMemberId);

    if (!day || !member) {
      setError("직원 또는 날짜 정보를 확인해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const created = await createShift(workplaceId, scheduleId, {
        memberId: member.id,
        startAt: `${day.date}T${startTime}:00`,
        endAt: `${day.date}T${endTime}:00`,
        confirmUnavailableConflict,
      });

      // 서버가 내려준 값을 화면 표시용 Shift 형태로 변환해 반영
      const newShift: Shift = {
        id: created.shiftId,
        memberId: member.id,
        memberName: created.memberName,
        day: selectedDay,
        startTime,
        endTime,
      };

      setShifts((prev) => [...prev, newShift]);
      closeAfterSuccess();
    } catch (err) {
      if (err instanceof ApiError) {
        // 직원의 불가능 시간과 겹치는 경우, 관리자에게 강행 여부를 확인
        if (
            err.code === UNAVAILABLE_TIME_CONFLICT_CODE &&
            !confirmUnavailableConflict
        ) {
          const confirmed = window.confirm(
              `${err.message}\n\n그래도 근무를 등록하시겠습니까?`
          );

          if (confirmed) {
            await submitCreateShift(true);
            return;
          }

          setError("근무 등록이 취소되었습니다.");
        } else {
          setError(err.message);
        }
      } else {
        setError("근무 등록 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSaveShift = async () => {
    if (startTime >= endTime) {
      setError("종료 시간은 시작 시간보다 늦어야 합니다.");
      return;
    }

    // 1. 근무 수정 (editingShiftId가 있는 경우)
    if (editingShiftId !== null) {
      if (scheduleId === null) {
        setError("근무표가 생성되지 않았습니다.");
        return;
      }

      const day = days.find((d) => d.key === selectedDay);
      const member = members.find((m) => m.id === selectedMemberId);

      if (!day || !member) {
        setError("직원 또는 날짜 정보를 확인해주세요.");
        return;
      }

      setIsSubmitting(true);
      setError("");

      //SFT-015 발생 시 재요청을 처리하기 위한 내부 함수 구현
      const submitUpdateShift = async (confirmUnavailableConflict: boolean) => {
        try {
          await updateShift(workplaceId, scheduleId, editingShiftId, {
            memberId: member.id,
            startAt: `${day.date}T${startTime}:00`,
            endAt: `${day.date}T${endTime}:00`,
            confirmUnavailableConflict,
          });

          // 프론트 State 업데이트
          setShifts((prevShifts) =>
              prevShifts.map((shift) =>
                  shift.id === editingShiftId
                      ? {
                        ...shift,
                        memberId: member.id,
                        memberName: member.name,
                        day: selectedDay,
                        startTime,
                        endTime,
                      }
                      : shift
              )
          );

          closeAfterSuccess();
        } catch (err) {
          if (err instanceof ApiError) {
            // 직원의 불가능 시간과 겹치는 경우, 관리자에게 강행 여부를 확인
            if (
                err.code === UNAVAILABLE_TIME_CONFLICT_CODE &&
                !confirmUnavailableConflict
            ) {
              const confirmed = window.confirm(
                  `${err.message}\n\n그래도 근무를 수정하시겠습니까?`
              );

              if (confirmed) {
                //confirmUnavailableConflict: true로 재요청
                await submitUpdateShift(true);
                return;
              }

              setError("근무 수정이 취소되었습니다.");
            } else {
              setError(err.message);
            }
          } else {
            setError("수정 중 오류가 발생했습니다.");
          }
        }
      };

      try {
        await submitUpdateShift(false);
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    // 2. 새 근무 추가 (editingShiftId가 null인 경우)
    await submitCreateShift(false);
  };

 //근무 삭제 핸들러 구현
  const handleDeleteShift = async () => {
    if (editingShiftId === null || scheduleId === null) return;

    if (!window.confirm("정말로 이 근무를 삭제하시겠습니까?")) return;

    setIsSubmitting(true);
    setError("");

    try {
      await deleteShift(workplaceId, scheduleId, editingShiftId);

      // 삭제된 근무 State에서 제거
      setShifts((prev) => prev.filter((shift) => shift.id !== editingShiftId));
      closeAfterSuccess();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("삭제 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };
  return (
      <>
        <PageHeader
            title="주간 근무표"
            description="이번 주 근무 일정을 확인하고 관리하세요."
        >
          {status === "NONE" && (
              <button
                  type="button"
                  onClick={handleCreateSchedule}
                  className="rounded-xl bg-[#005642] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
              >
                이번 주 근무표 생성
              </button>
          )}

          {status === "DRAFT" && (
              <div className="flex gap-2">
                <button
                    type="button"
                    onClick={openAddShiftModal}
                    className="rounded-xl border border-[#dce8e2] bg-white px-4 py-2.5 text-sm font-bold text-[#005642] transition hover:bg-[#f3fbf7]"
                >
                  + 근무 추가
                </button>

                <button
                    type="button"
                    onClick={handleTogglePublish}
                    className="rounded-xl bg-[#005642] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                >
                  공개하기
                </button>
              </div>
          )}

          {status === "PUBLISHED" && (
              <button
                  type="button"
                  onClick={handleTogglePublish}
                  className="rounded-xl border border-[#dce8e2] bg-white px-4 py-2.5 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                미공개로 전환
              </button>
          )}
        </PageHeader>

        <Card>
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <button
                  type="button"
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">‹</span>
              </button>

              <p className="font-bold">2026년 9월 3주차</p>

              <button
                  type="button"
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">›</span>
              </button>

              {status === "DRAFT" && (
                  <span className="ml-2 rounded-full bg-[#fff1d7] px-3 py-1 text-xs font-bold text-[#a96d09]">
                미공개
              </span>
              )}

              {status === "PUBLISHED" && (
                  <span className="ml-2 rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                공개됨
              </span>
              )}
            </div>

            {status === "DRAFT" && (
                <p className="text-xs text-[#78847f]">
                  직원에게 공개되기 전 근무표입니다.
                </p>
            )}

            {status === "PUBLISHED" && (
                <p className="text-xs font-semibold text-[#14956c]">
                  직원에게 공개된 근무표입니다.
                </p>
            )}
          </div>

          {status === "NONE" ? (
              <div className="flex min-h-[420px] flex-col items-center justify-center rounded-xl border border-dashed border-[#c9ded4] bg-[#fafdfb] px-4 text-center">
                <div className="text-4xl">📅</div>

                <h2 className="mt-4 text-lg font-black">
                  아직 이번 주 근무표가 없어요.
                </h2>

                <p className="mt-2 max-w-md text-sm leading-6 text-[#78847f]">
                  등록된 정기 근무를 기준으로 이번 주 근무표 초안을 생성할 수
                  있습니다.
                </p>

                <button
                    type="button"
                    onClick={handleCreateSchedule}
                    className="mt-5 rounded-xl bg-[#005642] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                >
                  이번 주 근무표 생성
                </button>
              </div>
          ) : (
              <>
                <div className="overflow-x-auto">
                  <div className="min-w-[1050px] overflow-hidden rounded-xl border border-[#dce8e2]">
                    {/* 날짜 헤더 */}
                    <div className="grid grid-cols-[80px_repeat(7,1fr)] bg-[#fafcfb]">
                      <div className="border-r border-[#edf2ef] p-3" />

                      {days.map((day) => (
                          <div
                              key={day.key}
                              className="border-r border-[#edf2ef] p-3 text-center text-sm font-bold last:border-r-0"
                          >
                            {day.label}
                          </div>
                      ))}
                    </div>

                    {/* 시간표 */}
                    <div className="grid grid-cols-[80px_repeat(7,1fr)]">
                      {/* 시간 영역 */}
                      <div
                          className="relative border-r border-[#edf2ef] bg-[#fafcfb]"
                          style={{
                            height: (END_HOUR - START_HOUR) * HOUR_HEIGHT,
                          }}
                      >
                        {timeLabels.map((time, index) => (
                            <div
                                key={time}
                                className="absolute right-3 -translate-y-1/2 text-xs font-semibold text-[#78847f]"
                                style={{ top: index * HOUR_HEIGHT }}
                            >
                              {time}
                            </div>
                        ))}
                      </div>

                      {/* 요일 영역 */}
                      {days.map((day) => {
                        const dayShifts = getPositionedShifts(
                            shifts.filter((shift) => shift.day === day.key)
                        );

                        return (
                            <div
                                key={day.key}
                                className="relative border-r border-[#edf2ef] last:border-r-0"
                                style={{
                                  height: (END_HOUR - START_HOUR) * HOUR_HEIGHT,
                                }}
                            >
                              {/* 시간 구분선 */}
                              {timeLabels.map((time, index) => (
                                  <div
                                      key={time}
                                      className="absolute left-0 right-0 border-t border-[#edf2ef]"
                                      style={{ top: index * HOUR_HEIGHT }}
                                  />
                              ))}

                              {/* 근무 카드 */}
                              {dayShifts.map((shift) => (
                                  <button
                                      key={shift.id}
                                      type="button"
                                      onClick={() => openEditShiftModal(shift)}
                                      disabled={status !== "DRAFT"}
                                      style={getShiftStyle(shift)}
                                      className="absolute z-10 overflow-hidden rounded-lg border border-[#bde5d3] bg-[#dff7ec] px-2 py-2 text-left text-[#005642] shadow-sm transition hover:bg-[#cceedd] disabled:cursor-default disabled:hover:bg-[#dff7ec]"
                                  >
                                    <p className="truncate text-sm font-black">
                                      {shift.memberName}
                                    </p>

                                    <p className="mt-1 text-[11px] font-semibold text-[#397660]">
                                      {shift.startTime} ~ {shift.endTime}
                                    </p>

                                    {status === "DRAFT" && (
                                        <p className="mt-2 text-[10px] text-[#5d8f7b]">
                                          클릭하여 수정
                                        </p>
                                    )}
                                  </button>
                              ))}
                            </div>
                        );
                      })}
                    </div>
                  </div>
                </div>

                <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex gap-4 text-xs text-[#78847f]">
                    <div className="flex items-center gap-2">
                      <span className="h-3 w-3 rounded-sm bg-[#dff7ec]" />
                      등록된 근무
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="h-3 w-3 rounded-sm border border-[#dce8e2] bg-white" />
                      근무 없음
                    </div>
                  </div>

                  {status === "DRAFT" && (
                      <p className="text-xs text-[#78847f]">
                        근무 카드를 클릭하면 내용을 수정할 수 있어요.
                      </p>
                  )}

                  {status === "PUBLISHED" && (
                      <p className="text-xs font-semibold text-[#14956c]">
                        공개 완료된 근무표입니다.
                      </p>
                  )}
                </div>
              </>
          )}
        </Card>

        {/* 근무 추가 / 수정 모달 */}
        {showShiftModal && status === "DRAFT" && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-black">
                      {editingShiftId !== null ? "근무 수정" : "근무 추가"}
                    </h2>

                    <p className="mt-1 text-sm text-[#78847f]">
                      {editingShiftId !== null
                          ? "근무자와 근무 시간을 수정해주세요."
                          : "근무자와 근무 시간을 선택해주세요."}
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={closeShiftModal}
                      className="text-xl text-[#78847f] hover:text-black"
                  >
                    ×
                  </button>
                </div>

                <div className="mt-6 space-y-5">
                  <div>
                    <label className="mb-2 block text-sm font-bold">
                      직원
                    </label>

                    <select
                        value={selectedMemberId}
                        onChange={(e) => setSelectedMemberId(Number(e.target.value))}
                        className="w-full rounded-xl border border-[#dce8e2] bg-white px-4 py-3 outline-none focus:border-[#14956c]"
                    >
                      {members.map((member) => (
                          <option key={member.id} value={member.id}>
                            {member.name}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="mb-2 block text-sm font-bold">
                      날짜
                    </label>

                    <select
                        value={selectedDay}
                        onChange={(e) => setSelectedDay(e.target.value as DayKey)}
                        className="w-full rounded-xl border border-[#dce8e2] bg-white px-4 py-3 outline-none focus:border-[#14956c]"
                    >
                      {days.map((day) => (
                          <option key={day.key} value={day.key}>
                            {day.label}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="mb-2 block text-sm font-bold">
                      근무 시간
                    </label>

                    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
                      <input
                          type="time"
                          value={startTime}
                          onChange={(e) => setStartTime(e.target.value)}
                          disabled={isSubmitting}
                          className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                      />

                      <span className="text-[#78847f]">~</span>

                      <input
                          type="time"
                          value={endTime}
                          onChange={(e) => setEndTime(e.target.value)}
                          disabled={isSubmitting}
                          className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                      />
                    </div>
                  </div>

                  {error && (
                      <p className="text-sm font-semibold text-[#d95555]">
                        {error}
                      </p>
                  )}
                </div>

                <div className="mt-6 flex gap-3">
                  {/* 수정 모드일 때만 '삭제' 버튼 표시 */}
                  {editingShiftId !== null && (
                      <button
                          type="button"
                          onClick={handleDeleteShift}
                          disabled={isSubmitting}
                          className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-600 transition hover:bg-red-100 disabled:opacity-50"
                      >
                        삭제
                      </button>
                  )}
                  <button
                      type="button"
                      onClick={closeShiftModal}
                      disabled={isSubmitting}
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    취소
                  </button>

                  <button
                      type="button"
                      onClick={handleSaveShift}
                      disabled={isSubmitting}
                      className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                  >
                    {isSubmitting ? "처리 중..." : editingShiftId !== null
                            ? "수정 완료" : "근무 추가"}
                  </button>
                </div>
              </div>
            </div>
        )}
      </>
  );
}