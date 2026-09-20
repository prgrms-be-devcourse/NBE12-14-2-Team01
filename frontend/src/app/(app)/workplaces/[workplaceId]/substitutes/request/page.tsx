"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type MyShift = {
  id: number;
  date: string;
  day: string;
  startTime: string;
  endTime: string;
  workplaceName: string;
};

const myShifts: MyShift[] = [
  {
    id: 101,
    date: "9월 21일",
    day: "월",
    startTime: "09:00",
    endTime: "18:00",
    workplaceName: "카페 스위치",
  },
  {
    id: 102,
    date: "9월 23일",
    day: "수",
    startTime: "09:00",
    endTime: "18:00",
    workplaceName: "카페 스위치",
  },
];

export default function SubstituteRequestPage() {
  const [selectedShiftId, setSelectedShiftId] =
      useState(myShifts[0]?.id ?? 0);

  const [error, setError] =
      useState("");

  const [successMessage, setSuccessMessage] =
      useState("");

  const selectedShift = myShifts.find(
      (shift) => shift.id === selectedShiftId
  );

  const handleSubmit = () => {
    if (!selectedShift) {
      setError(
          "대체 근무를 요청할 근무를 선택해주세요."
      );
      return;
    }

    setError("");

    /*
     * TODO: API 연동 시
     * POST /api/v1/shifts/{shiftId}/substitute-requests
     *
     * 요청자는 로그인한 사용자,
     * 대체 근무 후보는 서버에서 자동으로 결정한다.
     *
     * 현재는 프론트 화면 확인을 위해
     * 성공 상태만 임시로 표시한다.
     */
    setSuccessMessage(
        "대체 근무 요청이 전송되었습니다."
    );
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

          <div className="mt-6">
            <label className="mb-2 block text-sm font-bold">
              대체가 필요한 근무
            </label>

            <select
                value={selectedShiftId}
                onChange={(e) => {
                  setSelectedShiftId(
                      Number(e.target.value)
                  );

                  setError("");
                  setSuccessMessage("");
                }}
                className="w-full rounded-xl border border-[#dce8e2] bg-white px-4 py-3 outline-none focus:border-[#14956c]"
            >
              {myShifts.map((shift) => (
                  <option
                      key={shift.id}
                      value={shift.id}
                  >
                    {shift.date} ({shift.day}){" "}
                    {shift.startTime} -{" "}
                    {shift.endTime} /{" "}
                    {shift.workplaceName}
                  </option>
              ))}
            </select>
          </div>

          {selectedShift && (
              <div className="mt-5 rounded-xl border border-[#dce8e2] bg-[#fafdfb] p-4">
                <p className="text-xs font-semibold text-[#78847f]">
                  선택한 근무
                </p>

                <p className="mt-2 font-black">
                  {selectedShift.date} (
                  {selectedShift.day})
                </p>

                <p className="mt-1 font-bold text-[#005642]">
                  {selectedShift.startTime} ~{" "}
                  {selectedShift.endTime}
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
                대체 근무가 가능한 후보는 시스템이
                자동으로 확인합니다.
              </p>

              <p>
                요청자는 후보 직원을 직접 선택하거나
                개별적으로 연락할 필요가 없습니다.
              </p>

              <p>
                후보 직원이 요청을 수락한 뒤 관리자
                최종 승인이 완료되어야 실제 근무자가
                변경됩니다.
              </p>
            </div>
          </div>

          {error && (
              <p className="mt-4 text-sm font-semibold text-[#d95555]">
                {error}
              </p>
          )}

          {successMessage && (
              <div className="mt-4 rounded-xl bg-[#dff7ec] px-4 py-3 text-sm font-bold text-[#14956c]">
                {successMessage}
              </div>
          )}

          <button
              type="button"
              onClick={handleSubmit}
              className="mt-5 w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52]"
          >
            대체 근무 요청 보내기
          </button>
        </Card>

        <Card className="mt-6">
          <div>
            <h2 className="text-lg font-black">
              요청은 어떻게 진행되나요?
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              별도의 연락 없이 시스템 안에서 대체 근무
              요청을 진행할 수 있어요.
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
                참여하기 어려운 내 공식 근무를
                선택합니다.
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
                시스템이 일정이 겹치지 않는 직원을
                찾아 요청을 전달합니다.
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
                후보가 수락하면 관리자 승인 후
                최종 근무자가 변경됩니다.
              </p>
            </div>
          </div>
        </Card>
      </>
  );
}