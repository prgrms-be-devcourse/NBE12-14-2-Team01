"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { apiFetch, ApiError } from "@/lib/api";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type DayKey = "월" | "화" | "수" | "목" | "금" | "토" | "일";


type RegularShiftPattern = {
  patternId: number;
  memberId: number;
  memberName: string;
  role: "MANAGER" | "EMPLOYEE";
  dayOfWeek:
    | "MONDAY"
    | "TUESDAY"
    | "WEDNESDAY"
    | "THURSDAY"
    | "FRIDAY"
    | "SATURDAY"
    | "SUNDAY";
  startTime: string;
  endTime: string;
};

type WorkplaceMember = {
  memberId: number;
  name: string;
  role: "MANAGER" | "EMPLOYEE";
};

type RegularShiftPatternRequest = {
  memberId: number;
  dayOfWeek: RegularShiftPattern["dayOfWeek"];
  startTime: string;
  endTime: string;
};

const days: DayKey[] = ["월", "화", "수", "목", "금", "토", "일"];

const dayToApi: Record<DayKey, RegularShiftPattern["dayOfWeek"]> = {
  월: "MONDAY",
  화: "TUESDAY",
  수: "WEDNESDAY",
  목: "THURSDAY",
  금: "FRIDAY",
  토: "SATURDAY",
  일: "SUNDAY",
};

const apiToDay: Record<RegularShiftPattern["dayOfWeek"], DayKey> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일",
};

const apiToKorean: Record<RegularShiftPattern["dayOfWeek"], string> = {
  MONDAY: "월요일",
  TUESDAY: "화요일",
  WEDNESDAY: "수요일",
  THURSDAY: "목요일",
  FRIDAY: "금요일",
  SATURDAY: "토요일",
  SUNDAY: "일요일",
};



export default function RegularShiftsPage() {
  const params = useParams();
  const workplaceId = params.workplaceId as string;
  const [members, setMembers] = useState<WorkplaceMember[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [patterns, setPatterns] = useState<RegularShiftPattern[]>([]);
  const [editingPatternId, setEditingPatternId] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [selectedDays, setSelectedDays] = useState<DayKey[]>([]);
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("18:00");

useEffect(() => {
  const fetchData = async () => {
    try {
      // 전체 직원 조회
      const memberData = await apiFetch<WorkplaceMember[]>(
        `/workplaces/${workplaceId}/members`
      );

      // 전체 정기근무 조회
      const patternData = await apiFetch<RegularShiftPattern[]>(
        `/workplaces/${workplaceId}/regular-shift-patterns`
      );

      console.log("직원 목록:", memberData);
      console.log("정기근무 목록:", patternData);

      setMembers(memberData);
      setPatterns(patternData);
    } catch (error) {
      console.error("정기근무 조회 오류:", error);

      if (error instanceof ApiError) {
        alert(error.message);
      } else {
        alert("정기 근무 목록을 불러오지 못했습니다.");
      }
    }
  };

  if (workplaceId) {
    fetchData();
  }
}, [workplaceId]);

  const handleDayClick = (day: DayKey) => {
    if (editingPatternId !== null) {
    setSelectedDays([day]);
    return;
  }

    if (selectedDays.includes(day)) {
      setSelectedDays(
          selectedDays.filter((selectedDay) => selectedDay !== day)
      );
      return;
    }

    setSelectedDays([...selectedDays, day]);
  };

const handleRegister = async () => {
  if (selectedMemberId === null || selectedDays.length === 0) {
    return;
  }

  if (startTime >= endTime) {
    alert("종료 시간은 시작 시간보다 늦어야 합니다.");
    return;
  }

  if (isSubmitting) {
    return;
  }

  setIsSubmitting(true);

  try {
    // 수정 모드
    if (editingPatternId !== null) {
      const day = selectedDays[0];

      const updated = await apiFetch<RegularShiftPattern>(
        `/workplaces/${workplaceId}/regular-shift-patterns/${editingPatternId}`,
        {
          method: "PUT",
          body: JSON.stringify({
            memberId: selectedMemberId,
            dayOfWeek: dayToApi[day],
            startTime,
            endTime,
          }),
        }
      );

      setPatterns((prev) =>
        prev.map((pattern) =>
          pattern.patternId === editingPatternId
            ? {
                ...pattern,
                ...updated,
              }
            : pattern
        )
      );

      resetEditMode();

      alert("정기 근무가 수정되었습니다.");
      return;
    }

    // 등록 모드
const succeededDays: DayKey[] = [];

  for (const day of selectedDays) {
    try {
      const created = await apiFetch<RegularShiftPattern>(
        `/workplaces/${workplaceId}/regular-shift-patterns`,
        {
          method: "POST",
          body: JSON.stringify({
            memberId: selectedMemberId,
            dayOfWeek: dayToApi[day],
            startTime,
            endTime,
          }),
        }
      );

      setPatterns((prev) => [...prev, created]);
      succeededDays.push(day);
    } catch (error) {
      // 이미 성공한 요일은 선택에서 제거
      setSelectedDays((prev) =>
        prev.filter((selectedDay) => !succeededDays.includes(selectedDay))
      );

      const succeededMessage =
        succeededDays.length > 0
          ? `등록 완료: ${succeededDays
              .map((succeededDay) => `${succeededDay}요일`)
              .join(", ")}\n`
          : "";

      if (error instanceof ApiError) {
        alert(
          `${succeededMessage}${day}요일 등록 실패: ${error.message}`
        );
      } else {
        alert(
          `${succeededMessage}${day}요일 등록 중 통신 오류가 발생했습니다.`
        );
      }

      return;
    }
  }

    setSelectedDays([]);
    alert("정기 근무가 등록되었습니다.");
    } catch (error) {
      console.error("정기근무 저장 오류:", error);

      if (error instanceof ApiError) {
        alert(error.message);
      } else {
        alert("정기 근무 처리 중 오류가 발생했습니다.");
    }
  } finally {
    setIsSubmitting(false);
  }
}

const handleDelete = async (patternId: number) => {
  const confirmed = confirm("이 정기 근무를 삭제하시겠습니까?");

  if (!confirmed) {
    return;
  }

  if (isSubmitting) {
    return;
  }

  setIsSubmitting(true);


  try {
    await apiFetch<void>(
      `/workplaces/${workplaceId}/regular-shift-patterns/${patternId}`,
      {
        method: "DELETE",
      }
    );

    setPatterns((prev) =>
      prev.filter((pattern) => pattern.patternId !== patternId)
    );

    if (editingPatternId === patternId) {
      resetEditMode();
    }

    alert("정기 근무가 삭제되었습니다.");
  } catch (error) {
    console.error("정기근무 삭제 오류:", error);

    if (error instanceof ApiError) {
      alert(error.message);
    } else {
      alert("정기 근무 삭제 중 오류가 발생했습니다.");
    }
  } finally {
    setIsSubmitting(false);
  }
}

const selectedMemberShifts = patterns.filter(
  (pattern) => pattern.memberId === selectedMemberId
);

const resetEditMode = () => {
  setEditingPatternId(null);
  setSelectedDays([]);
  setStartTime("09:00");
  setEndTime("18:00");
};

const handleEdit = (shift: RegularShiftPattern) => {
  setEditingPatternId(shift.patternId);
  setSelectedMemberId(shift.memberId);
  setSelectedDays([apiToDay[shift.dayOfWeek]]);
  setStartTime(shift.startTime);
  setEndTime(shift.endTime);
};

  return (
      <>
        <PageHeader
            title="정기 근무"
            description="직원별 반복 근무 요일과 시간을 등록하세요."
        />

        <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
          <Card>
            <h2 className="text-lg font-black">정기 근무 등록</h2>

            <p className="mt-1 text-sm text-[#78847f]">
              직원과 반복되는 근무 시간을 선택하세요.
            </p>

            <div className="mt-6 space-y-5">
              {/* 직원 선택 */}
              <div>
                <label className="mb-2 block text-sm font-bold">직원 선택</label>

                <div className="relative">
                  <select
                    value={selectedMemberId ?? ""}
                    disabled={isSubmitting}
                    onChange={(e) => {
                      setSelectedMemberId(
                        e.target.value === "" ? null : Number(e.target.value)
                      );
                      resetEditMode();
                    }}
                    className="w-full appearance-none rounded-xl border border-[#dce8e2] bg-white py-3 pl-4 pr-12 outline-none transition focus:border-[#14956c]"
                  >
                    <option value="">직원을 선택하세요</option>

                    {members.map((member) => (
                      <option
                        key={member.memberId}
                        value={member.memberId}
                      >
                        {member.name}
                      </option>
                    ))}
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

              {/* 근무 요일 */}
              <div>
                <label className="mb-2 block text-sm font-bold">근무 요일</label>

                <div className="grid grid-cols-7 gap-2">
                  {days.map((day) => {
                    const selected = selectedDays.includes(day);

                    return (
                        <button
                            key={day}
                            type="button"
                            disabled={isSubmitting}
                            onClick={() => handleDayClick(day)}
                            className={`rounded-xl border py-3 text-sm font-bold transition ${
                                selected
                                    ? "border-[#005642] bg-[#005642] text-white"
                                    : "border-[#dce8e2] text-[#66736d] hover:border-[#14956c] hover:bg-[#f3fbf7] hover:text-[#005642]"
                            }`}
                        >
                          {day}
                        </button>
                    );
                  })}
                </div>
              </div>

              {/* 근무 시간 */}
              <div>
                <label className="mb-2 block text-sm font-bold">근무 시간</label>

                <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
                  <input
                      type="time"
                      value={startTime}
                      disabled={isSubmitting}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                  />

                  <span className="text-[#78847f]">~</span>

                  <input
                      type="time"
                      value={endTime}
                      disabled={isSubmitting}
                      onChange={(e) => setEndTime(e.target.value)}
                      className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                  />
                </div>
              </div>

              <button
                  type="button"
                  onClick={handleRegister}
                  disabled={
                    selectedDays.length === 0 ||
                    selectedMemberId === null ||
                    isSubmitting
                  }
                  className="w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:bg-[#b8c4be]"
              >
                {isSubmitting
                  ? "처리 중..."
                  : editingPatternId !== null
                    ? "정기 근무 수정"
                    : "정기 근무 등록"}
              </button>

              {editingPatternId !== null && (
                  <button
                    type="button"
                    onClick={resetEditMode}
                    disabled={isSubmitting}
                    className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    수정 취소
                  </button>
                )}
            </div>
          </Card>

          <Card>
            <div className="mt-6 space-y-3">
              {selectedMemberId === null ? (
                <p className="text-sm text-[#78847f]">
                  직원을 선택해주세요.
                </p>
              ) : selectedMemberShifts.length === 0 ? (
                <p className="text-sm text-[#78847f]">
                  등록된 정기 근무가 없습니다.
                </p>
              ) : (
                selectedMemberShifts.map((shift) => (
                  <div
                    key={shift.patternId}
                    className="flex items-center justify-between rounded-xl border border-[#dce8e2] p-4"
                  >
                    <div>
                      <p className="font-bold">
                        {apiToKorean[shift.dayOfWeek]}
                      </p>

                      <p className="mt-1 text-sm text-[#78847f]">
                        {shift.startTime} - {shift.endTime}
                      </p>
                    </div>

                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleEdit(shift)}
                        disabled={isSubmitting}
                        className="rounded-lg border border-[#dce8e2] px-3 py-2 text-sm font-semibold text-[#66736d] hover:bg-[#f3fbf7]"
                      >
                        수정
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDelete(shift.patternId)}
                        disabled={isSubmitting}
                        className="rounded-lg border border-[#f1cccc] px-3 py-2 text-sm font-semibold text-[#d95555] hover:bg-[#fff5f5]"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      </>
  );
}