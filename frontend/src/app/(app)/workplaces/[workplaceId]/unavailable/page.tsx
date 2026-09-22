"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type UnavailableTime = {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
};

const initialUnavailableTimes: UnavailableTime[] = [
  {
    id: 1,
    date: "2026-09-18",
    startTime: "09:00",
    endTime: "18:00",
  },
  {
    id: 2,
    date: "2026-09-19",
    startTime: "13:00",
    endTime: "18:00",
  },
];

const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

function formatDate(date: string) {
  const [, month, day] = date.split("-");

  return `${Number(month)}월 ${Number(day)}일`;
}

export default function UnavailablePage() {
  const [currentYear, setCurrentYear] = useState(2026);
  const [currentMonth, setCurrentMonth] = useState(9);
  const [unavailableTimes, setUnavailableTimes] =
      useState<UnavailableTime[]>(initialUnavailableTimes);
  const [showModal, setShowModal] = useState(false);
  const [selectedDate, setSelectedDate] = useState("2026-09-18");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("18:00");
  const [error, setError] = useState("");

  const firstDay = new Date(
      currentYear,
      currentMonth - 1,
      1
  ).getDay();

  const lastDate = new Date(
      currentYear,
      currentMonth,
      0
  ).getDate();

  const calendarCells = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: lastDate }, (_, index) => index + 1),
  ];

  const getDateString = (day: number) => {
    return `${currentYear}-${String(currentMonth).padStart(
        2,
        "0"
    )}-${String(day).padStart(2, "0")}`;
  };

  const isUnavailableDate = (day: number) => {
    const date = getDateString(day);

    return unavailableTimes.some((item) => item.date === date);
  };

  const openAddModal = (date?: string) => {
    if (date) {
      setSelectedDate(date);
    } else {
      setSelectedDate(
          `${currentYear}-${String(currentMonth).padStart(2, "0")}-01`
      );
    }

    setStartTime("09:00");
    setEndTime("18:00");
    setError("");
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setError("");
  };

  const handleAddUnavailable = () => {
    if (startTime >= endTime) {
      setError("종료 시간은 시작 시간보다 늦어야 합니다.");
      return;
    }

    const newUnavailable: UnavailableTime = {
      id: Date.now(),
      date: selectedDate,
      startTime,
      endTime,
    };

    setUnavailableTimes((prev) => [
      ...prev,
      newUnavailable,
    ]);

    closeModal();
  };

  const handleDelete = (id: number) => {
    setUnavailableTimes((prev) =>
        prev.filter((item) => item.id !== id)
    );
  };

  const handlePrevMonth = () => {
    if (currentMonth === 1) {
      setCurrentYear(currentYear - 1);
      setCurrentMonth(12);
      return;
    }

    setCurrentMonth(currentMonth - 1);
  };

  const handleNextMonth = () => {
    if (currentMonth === 12) {
      setCurrentYear(currentYear + 1);
      setCurrentMonth(1);
      return;
    }

    setCurrentMonth(currentMonth + 1);
  };

  return (
      <>
        <PageHeader
            title="불가능 일정"
            description="대체 근무 요청을 받을 수 없는 시간을 등록하세요."
        >
          <button
              type="button"
              onClick={() => openAddModal()}
              className="rounded-xl bg-[#005642] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
          >
            + 불가능 일정 추가
          </button>
        </PageHeader>

        <Card>
          <div className="flex items-center justify-between">
            <button
                type="button"
                onClick={handlePrevMonth}
                className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
            >
              <span className="block -translate-y-px text-xl leading-none">‹</span>
            </button>

            <h2 className="text-lg font-black">
              {currentYear}년 {currentMonth}월
            </h2>

            <button
                type="button"
                onClick={handleNextMonth}
                className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
            >
              <span className="block -translate-y-px text-xl leading-none">›</span>
            </button>
          </div>

          <div className="mt-5 grid grid-cols-7 border-b border-[#edf2ef] pb-3 text-center text-sm font-semibold text-[#78847f]">
            {weekDays.map((day) => (
                <div key={day}>
                  {day}
                </div>
            ))}
          </div>

          <div className="mt-3 grid grid-cols-7">
            {calendarCells.map((day, index) => {
              if (!day) {
                return (
                    <div
                        key={`empty-${index}`}
                        className="min-h-[110px]"
                    />
                );
              }

              const date = getDateString(day);
              const unavailable = isUnavailableDate(day);

              return (
                  <button
                      key={date}
                      type="button"
                      onClick={() => openAddModal(date)}
                      className="flex min-h-[110px] items-center justify-center p-2"
                  >
                    <div
                        className={`flex h-full min-h-[90px] w-full items-center justify-center rounded-xl text-sm font-semibold transition ${
                            unavailable
                                ? "bg-[#005642] text-white"
                                : "text-[#25332d] hover:bg-[#f3fbf7]"
                        }`}
                    >
                      {day}
                    </div>
                  </button>
              );
            })}
          </div>

          <div className="mt-5 rounded-xl bg-[#f3fbf7] p-4">
            <p className="text-sm font-black text-[#005642]">
              불가능 일정이란?
            </p>

            <p className="mt-1 text-sm leading-6 text-[#66736d]">
              해당 시간에는 대체 근무 후보에서 제외됩니다. 등록하지 않은
              시간이라고 해서 근무 가능이 확정되는 것은 아니에요.
            </p>
          </div>
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">
              등록된 일정
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              등록한 불가능 시간을 확인하고 관리하세요.
            </p>
          </div>

          <div className="mt-5 space-y-3">
            {unavailableTimes.map((item) => (
                <div
                    key={item.id}
                    className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-[#dce8e2] p-4"
                >
                  <div>
                    <p className="font-black">
                      {formatDate(item.date)}
                    </p>

                    <p className="mt-1 text-sm text-[#78847f]">
                      {item.startTime} ~ {item.endTime}
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={() => handleDelete(item.id)}
                      className="rounded-lg border border-[#f1cccc] px-3 py-2 text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5]"
                  >
                    삭제
                  </button>
                </div>
            ))}

            {unavailableTimes.length === 0 && (
                <div className="flex min-h-[150px] items-center justify-center rounded-xl border border-dashed border-[#dce8e2] text-sm text-[#78847f]">
                  등록된 불가능 일정이 없습니다.
                </div>
            )}
          </div>
        </Card>

        {/* 불가능 일정 추가 모달 */}
        {showModal && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-black">
                      불가능 일정 추가
                    </h2>

                    <p className="mt-1 text-sm text-[#78847f]">
                      근무하기 어려운 날짜와 시간을 등록해주세요.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={closeModal}
                      className="text-xl text-[#78847f] hover:text-black"
                  >
                    ×
                  </button>
                </div>

                <div className="mt-6 space-y-5">
                  <div>
                    <label className="mb-2 block text-sm font-bold">
                      날짜
                    </label>

                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                    />
                  </div>

                  <div>
                    <label className="mb-2 block text-sm font-bold">
                      불가능 시간
                    </label>

                    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
                      <input
                          type="time"
                          value={startTime}
                          onChange={(e) => setStartTime(e.target.value)}
                          className="rounded-xl border border-[#dce8e2] px-4 py-3 outline-none focus:border-[#14956c]"
                      />

                      <span className="text-[#78847f]">
                    ~
                  </span>

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
                      onClick={closeModal}
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    취소
                  </button>

                  <button
                      type="button"
                      onClick={handleAddUnavailable}
                      className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                  >
                    등록하기
                  </button>
                </div>
              </div>
            </div>
        )}
      </>
  );
}