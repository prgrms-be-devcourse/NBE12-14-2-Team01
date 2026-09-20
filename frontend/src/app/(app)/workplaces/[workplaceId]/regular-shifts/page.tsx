"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type DayKey =
    | "월"
    | "화"
    | "수"
    | "목"
    | "금"
    | "토"
    | "일";

type RegisteredShift = {
  day: string;
  startTime: string;
  endTime: string;
};

const days: DayKey[] = [
  "월",
  "화",
  "수",
  "목",
  "금",
  "토",
  "일",
];

const dayNames: Record<
    DayKey,
    string
> = {
  월: "월요일",
  화: "화요일",
  수: "수요일",
  목: "목요일",
  금: "금요일",
  토: "토요일",
  일: "일요일",
};

// API 연동 후 선택한 직원의 정기 근무 목록으로 변경
const initialRegisteredShifts: RegisteredShift[] =
    [
      {
        day: "월요일",
        startTime: "09:00",
        endTime: "18:00",
      },
      {
        day: "수요일",
        startTime: "09:00",
        endTime: "18:00",
      },
      {
        day: "금요일",
        startTime: "09:00",
        endTime: "18:00",
      },
    ];

export default function RegularShiftsPage() {
  const [
    selectedDays,
    setSelectedDays,
  ] = useState<DayKey[]>([]);

  const [
    startTime,
    setStartTime,
  ] = useState("09:00");

  const [
    endTime,
    setEndTime,
  ] = useState("18:00");

  const [
    registeredShifts,
    setRegisteredShifts,
  ] = useState<RegisteredShift[]>(
      initialRegisteredShifts
  );

  const handleDayClick = (
      day: DayKey
  ) => {
    if (
        selectedDays.includes(day)
    ) {
      setSelectedDays(
          selectedDays.filter(
              (selectedDay) =>
                  selectedDay !== day
          )
      );

      return;
    }

    setSelectedDays([
      ...selectedDays,
      day,
    ]);
  };

  const handleRegister = () => {
    if (
        selectedDays.length === 0
    ) {
      return;
    }

    const updatedShifts = [
      ...registeredShifts,
    ];

    selectedDays.forEach(
        (day) => {
          const fullDay =
              dayNames[day];

          const existingIndex =
              updatedShifts.findIndex(
                  (shift) =>
                      shift.day === fullDay
              );

          const newShift: RegisteredShift =
              {
                day: fullDay,
                startTime,
                endTime,
              };

          if (
              existingIndex >= 0
          ) {
            updatedShifts[
                existingIndex
                ] = newShift;
          } else {
            updatedShifts.push(
                newShift
            );
          }
        }
    );

    setRegisteredShifts(
        updatedShifts
    );

    setSelectedDays([]);
  };

  return (
      <>
        <PageHeader
            title="정기 근무"
            description="직원별 반복 근무 요일과 시간을 등록하세요."
        />

        <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
          <Card>
            <h2 className="text-lg font-black">
              정기 근무 등록
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              직원과 반복되는 근무
              시간을 선택하세요.
            </p>

            <div className="mt-6 space-y-5">
              {/* 직원 선택 */}
              <div>
                <label className="mb-2 block text-sm font-bold">
                  직원 선택
                </label>

                <div className="relative">
                  <select className="w-full appearance-none rounded-xl border border-[#dce8e2] bg-white py-3 pl-4 pr-12 outline-none transition focus:border-[#14956c]">
                    <option>
                      김지연
                    </option>

                    <option>
                      이서연
                    </option>

                    <option>
                      박민수
                    </option>

                    <option>
                      최하은
                    </option>
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
                <label className="mb-2 block text-sm font-bold">
                  근무 요일
                </label>

                <div className="grid grid-cols-7 gap-2">
                  {days.map(
                      (day) => {
                        const selected =
                            selectedDays.includes(
                                day
                            );

                        return (
                            <button
                                key={day}
                                type="button"
                                onClick={() =>
                                    handleDayClick(
                                        day
                                    )
                                }
                                className={`rounded-xl border py-3 text-sm font-bold transition ${
                                    selected
                                        ? "border-[#005642] bg-[#005642] text-white"
                                        : "border-[#dce8e2] text-[#66736d] hover:border-[#14956c] hover:bg-[#f3fbf7] hover:text-[#005642]"
                                }`}
                            >
                              {day}
                            </button>
                        );
                      }
                  )}
                </div>
              </div>

              {/* 근무 시간 */}
              <div>
                <label className="mb-2 block text-sm font-bold">
                  근무 시간
                </label>

                <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
                  <input
                      type="time"
                      value={startTime}
                      onChange={(e) =>
                          setStartTime(
                              e.target.value
                          )
                      }
                      className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                  />

                  <span className="text-[#78847f]">
                  ~
                </span>

                  <input
                      type="time"
                      value={endTime}
                      onChange={(e) =>
                          setEndTime(
                              e.target.value
                          )
                      }
                      className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                  />
                </div>
              </div>

              <button
                  type="button"
                  onClick={
                    handleRegister
                  }
                  disabled={
                      selectedDays.length ===
                      0
                  }
                  className="w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:bg-[#b8c4be]"
              >
                정기 근무 등록
              </button>
            </div>
          </Card>

          <Card>
            <div>
              <h2 className="text-lg font-black">
                등록된 정기 근무
              </h2>

              <p className="mt-1 text-sm text-[#78847f]">
                현재 선택한 직원의
                반복 근무입니다.
              </p>
            </div>

            <div className="mt-6 space-y-3">
              {registeredShifts.map(
                  (shift) => (
                      <div
                          key={shift.day}
                          className="flex items-center justify-between rounded-xl border border-[#dce8e2] p-4"
                      >
                        <div>
                          <p className="font-bold">
                            {shift.day}
                          </p>

                          <p className="mt-1 text-sm text-[#78847f]">
                            {
                              shift.startTime
                            }{" "}
                            -{" "}
                            {
                              shift.endTime
                            }
                          </p>
                        </div>

                        <div className="flex gap-2">
                          <button
                              type="button"
                              className="rounded-lg border border-[#dce8e2] px-3 py-2 text-sm font-semibold text-[#66736d] hover:bg-[#f3fbf7]"
                          >
                            수정
                          </button>

                          <button
                              type="button"
                              className="rounded-lg border border-[#f1cccc] px-3 py-2 text-sm font-semibold text-[#d95555] hover:bg-[#fff5f5]"
                          >
                            삭제
                          </button>
                        </div>
                      </div>
                  )
              )}
            </div>
          </Card>
        </div>
      </>
  );
}