"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

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
  memberName: string;
  day: DayKey;
  startTime: string;
  endTime: string;
};

type PositionedShift = Shift & {
  lane: number;
  laneCount: number;
};

const days: { key: DayKey; label: string }[] = [
  { key: "mon", label: "9/14 월" },
  { key: "tue", label: "9/15 화" },
  { key: "wed", label: "9/16 수" },
  { key: "thu", label: "9/17 목" },
  { key: "fri", label: "9/18 금" },
  { key: "sat", label: "9/19 토" },
  { key: "sun", label: "9/20 일" },
];

const members = ["김지연", "이서연", "박민수", "최하은"];

// TODO: API 연동 후 SCH-01 응답으로 대체
// 현재는 정기 근무를 기준으로 주간 근무표가 생성됐다고 가정한 mock 데이터
const generatedShifts: Shift[] = [
  {
    id: 1,
    memberName: "박민수",
    day: "mon",
    startTime: "14:00",
    endTime: "22:00",
  },
  {
    id: 2,
    memberName: "김지연",
    day: "tue",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 3,
    memberName: "김지연",
    day: "wed",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 4,
    memberName: "이서연",
    day: "wed",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 5,
    memberName: "박민수",
    day: "thu",
    startTime: "14:00",
    endTime: "22:00",
  },
  {
    id: 6,
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

export default function SchedulePage() {
  const [status, setStatus] = useState<ScheduleStatus>("NONE");
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [showShiftModal, setShowShiftModal] = useState(false);
  const [editingShiftId, setEditingShiftId] = useState<number | null>(null);
  const [selectedMember, setSelectedMember] = useState("김지연");
  const [selectedDay, setSelectedDay] = useState<DayKey>("mon");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("18:00");
  const [error, setError] = useState("");

  // TODO: API 연동 후 SCH-01 호출로 변경
  const handleCreateSchedule = () => {
    setShifts(generatedShifts);
    setStatus("DRAFT");
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
    setSelectedMember("김지연");
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
    setSelectedMember(shift.memberName);
    setSelectedDay(shift.day);
    setStartTime(shift.startTime);
    setEndTime(shift.endTime);
    setError("");
    setShowShiftModal(true);
  };

  const closeShiftModal = () => {
    setShowShiftModal(false);
    setEditingShiftId(null);
    setError("");
  };

  const handleSaveShift = () => {
    if (startTime >= endTime) {
      setError("종료 시간은 시작 시간보다 늦어야 합니다.");
      return;
    }

    // 기존 근무 수정
    if (editingShiftId !== null) {
      setShifts((prevShifts) =>
          prevShifts.map((shift) =>
              shift.id === editingShiftId
                  ? {
                    ...shift,
                    memberName: selectedMember,
                    day: selectedDay,
                    startTime,
                    endTime,
                  }
                  : shift
          )
      );
    } else {
      // 새 근무 추가
      const newShift: Shift = {
        id: Date.now(),
        memberName: selectedMember,
        day: selectedDay,
        startTime,
        endTime,
      };

      setShifts((prevShifts) => [...prevShifts, newShift]);
    }

    closeShiftModal();
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
                        value={selectedMember}
                        onChange={(e) => setSelectedMember(e.target.value)}
                        className="w-full rounded-xl border border-[#dce8e2] bg-white px-4 py-3 outline-none focus:border-[#14956c]"
                    >
                      {members.map((member) => (
                          <option key={member} value={member}>
                            {member}
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
                          className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                      />

                      <span className="text-[#78847f]">~</span>

                      <input
                          type="time"
                          value={endTime}
                          onChange={(e) => setEndTime(e.target.value)}
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
                  <button
                      type="button"
                      onClick={closeShiftModal}
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    취소
                  </button>

                  <button
                      type="button"
                      onClick={handleSaveShift}
                      className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                  >
                    {editingShiftId !== null ? "수정 완료" : "근무 추가"}
                  </button>
                </div>
              </div>
            </div>
        )}
      </>
  );
}