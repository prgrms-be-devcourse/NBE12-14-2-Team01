"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type MyShift = {
  id: number;
  date: string;
  day: string;
  dayNumber: string;
  startTime: string;
  endTime: string;
  status: "SCHEDULED";
};

const shifts: MyShift[] = [
  {
    id: 1,
    date: "9월 21일",
    day: "월",
    dayNumber: "21",
    startTime: "09:00",
    endTime: "18:00",
    status: "SCHEDULED",
  },
  {
    id: 2,
    date: "9월 23일",
    day: "수",
    dayNumber: "23",
    startTime: "09:00",
    endTime: "18:00",
    status: "SCHEDULED",
  },
];

function getWorkHours(startTime: string, endTime: string) {
  const [startHour, startMinute] = startTime.split(":").map(Number);
  const [endHour, endMinute] = endTime.split(":").map(Number);

  const start = startHour * 60 + startMinute;
  const end = endHour * 60 + endMinute;

  return (end - start) / 60;
}

export default function MyShiftsPage() {
  const params = useParams();
  const workplaceId = params.workplaceId as string;

  const totalHours = shifts.reduce(
      (total, shift) =>
          total + getWorkHours(shift.startTime, shift.endTime),
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
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">‹</span>
              </button>

              <p className="font-bold">
                2026년 9월 4주차
              </p>

              <button
                  type="button"
                  className="grid h-9 w-9 place-items-center rounded-lg border border-[#dce8e2] text-[#66736d] transition hover:bg-[#f3fbf7]"
              >
                <span className="block -translate-y-px text-xl leading-none">›</span>
              </button>
            </div>

            <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
            공개됨
          </span>
          </div>

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

          {shifts.length === 0 && (
              <div className="flex min-h-[180px] items-center justify-center rounded-xl border border-dashed border-[#dce8e2] text-sm text-[#78847f]">
                이번 주 예정된 근무가 없습니다.
              </div>
          )}
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">
              이번 주 요약
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              예정된 근무를 간단히 확인하세요.
            </p>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">
                총 근무
              </p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {shifts.length}회
              </p>
            </div>

            <div className="rounded-xl bg-[#f3fbf7] p-4">
              <p className="text-sm text-[#78847f]">
                예정 시간
              </p>

              <p className="mt-1 text-2xl font-black text-[#005642]">
                {totalHours}시간
              </p>
            </div>
          </div>

          <div className="mt-6 border-t border-[#edf2ef] pt-6">
            <p className="font-bold">
              근무가 어려우신가요?
            </p>

            <p className="mt-1 text-sm text-[#78847f]">
              예정된 근무 중 참여하기 어려운 일정이 있다면 대체 근무를
              요청할 수 있어요.
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