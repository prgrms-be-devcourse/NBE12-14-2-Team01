"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type RequestStatus =
    | "OPEN"
    | "ACCEPTED"
    | "APPROVED"
    | "CLOSED";

type SubstituteRequest = {
  id: number;
  requesterName: string;
  acceptedCandidateName: string | null;
  date: string;
  day: string;
  startTime: string;
  endTime: string;
  status: RequestStatus;
};

type Tab =
    | "WAITING"
    | "APPROVAL"
    | "COMPLETED";

const initialRequests: SubstituteRequest[] = [
  {
    id: 1,
    requesterName: "김지연",
    acceptedCandidateName: "이서연",
    date: "9월 21일",
    day: "월",
    startTime: "09:00",
    endTime: "18:00",
    status: "ACCEPTED",
  },
  {
    id: 2,
    requesterName: "박민수",
    acceptedCandidateName: null,
    date: "9월 23일",
    day: "수",
    startTime: "09:00",
    endTime: "15:00",
    status: "OPEN",
  },
  {
    id: 3,
    requesterName: "최하은",
    acceptedCandidateName: "박민수",
    date: "9월 19일",
    day: "토",
    startTime: "10:00",
    endTime: "16:00",
    status: "APPROVED",
  },
];

function getInitial(name: string) {
  return name.slice(0, 1);
}

export default function SubstituteAdminPage() {
  const [requests, setRequests] =
      useState<SubstituteRequest[]>(
          initialRequests
      );

  const [tab, setTab] =
      useState<Tab>("APPROVAL");

  const [
    selectedRequest,
    setSelectedRequest,
  ] =
      useState<SubstituteRequest | null>(
          null
      );

  const handleApprove = (
      requestId: number
  ) => {
    setRequests((prev) =>
        prev.map((request) =>
            request.id === requestId
                ? {
                  ...request,
                  status: "APPROVED",
                }
                : request
        )
    );

    setSelectedRequest(null);
  };

  const handleClose = (
      requestId: number
  ) => {
    setRequests((prev) =>
        prev.map((request) =>
            request.id === requestId
                ? {
                  ...request,
                  status: "CLOSED",
                }
                : request
        )
    );

    setSelectedRequest(null);
  };

  const waitingCount =
      requests.filter(
          (request) =>
              request.status === "OPEN"
      ).length;

  const approvalCount =
      requests.filter(
          (request) =>
              request.status ===
              "ACCEPTED"
      ).length;

  const completedCount =
      requests.filter(
          (request) =>
              request.status ===
              "APPROVED" ||
              request.status ===
              "CLOSED"
      ).length;

  const filteredRequests =
      requests.filter((request) => {
        if (tab === "WAITING") {
          return (
              request.status === "OPEN"
          );
        }

        if (tab === "APPROVAL") {
          return (
              request.status ===
              "ACCEPTED"
          );
        }

        return (
            request.status ===
            "APPROVED" ||
            request.status ===
            "CLOSED"
        );
      });

  return (
      <>
        <PageHeader
            title="승인 관리"
            description="직원이 수락한 대체 근무 요청을 확인하고 최종 승인하세요."
        />

        {/* 탭 */}
        <div className="mb-5 flex flex-wrap gap-2">
          <button
              type="button"
              onClick={() =>
                  setTab("WAITING")
              }
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "WAITING"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            수락 대기 {waitingCount}
          </button>

          <button
              type="button"
              onClick={() =>
                  setTab("APPROVAL")
              }
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "APPROVAL"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            승인 필요 {approvalCount}
          </button>

          <button
              type="button"
              onClick={() =>
                  setTab("COMPLETED")
              }
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "COMPLETED"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            처리 완료 {completedCount}
          </button>
        </div>

        <Card>
          {/* 데스크톱 테이블 */}
          <div className="overflow-x-auto">
            <div className="min-w-[850px]">
              <div className="grid grid-cols-[1.25fr_1fr_1fr_1fr_1fr] border-b border-[#edf2ef] px-3 pb-3 text-sm font-bold text-[#78847f]">
                <div>
                  근무 일시
                </div>

                <div>
                  요청자
                </div>

                <div>
                  수락자
                </div>

                <div>
                  상태
                </div>

                <div className="text-right">
                  관리
                </div>
              </div>

              {filteredRequests.map(
                  (request) => (
                      <div
                          key={request.id}
                          className="grid min-h-[88px] grid-cols-[1.25fr_1fr_1fr_1fr_1fr] items-center border-b border-[#edf2ef] px-3 last:border-b-0"
                      >
                        {/* 근무 일시 */}
                        <div>
                          <p className="font-black">
                            {request.date} (
                            {request.day})
                          </p>

                          <p className="mt-1 text-sm text-[#78847f]">
                            {
                              request.startTime
                            }{" "}
                            - {request.endTime}
                          </p>
                        </div>

                        {/* 요청자 */}
                        <div className="flex items-center gap-3">
                          <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[#f3fbf7] font-black text-[#005642]">
                            {getInitial(
                                request.requesterName
                            )}
                          </div>

                          <p className="font-bold">
                            {
                              request.requesterName
                            }
                          </p>
                        </div>

                        {/* 수락자 */}
                        <div>
                          {request.acceptedCandidateName ? (
                              <div className="flex items-center gap-3">
                                <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[#dff7ec] font-black text-[#005642]">
                                  {getInitial(
                                      request.acceptedCandidateName
                                  )}
                                </div>

                                <p className="font-bold">
                                  {
                                    request.acceptedCandidateName
                                  }
                                </p>
                              </div>
                          ) : (
                              <p className="text-sm text-[#9aa5a0]">
                                아직 없음
                              </p>
                          )}
                        </div>

                        {/* 상태 */}
                        <div>
                          {request.status ===
                              "OPEN" && (
                                  <span className="rounded-full bg-[#e7efff] px-3 py-1 text-xs font-bold text-[#4a73c9]">
                        수락 대기
                      </span>
                              )}

                          {request.status ===
                              "ACCEPTED" && (
                                  <span className="rounded-full bg-[#fff1d7] px-3 py-1 text-xs font-bold text-[#a96d09]">
                        승인 필요
                      </span>
                              )}

                          {request.status ===
                              "APPROVED" && (
                                  <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                        승인 완료
                      </span>
                              )}

                          {request.status ===
                              "CLOSED" && (
                                  <span className="rounded-full bg-[#f1f3f2] px-3 py-1 text-xs font-bold text-[#78847f]">
                        종료됨
                      </span>
                              )}
                        </div>

                        {/* 관리 */}
                        <div className="flex justify-end gap-2">
                          {request.status ===
                              "ACCEPTED" && (
                                  <>
                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleClose(
                                                request.id
                                            )
                                        }
                                        className="rounded-lg border border-[#f1cccc] px-3 py-2 text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5]"
                                    >
                                      종료
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleApprove(
                                                request.id
                                            )
                                        }
                                        className="rounded-lg bg-[#005642] px-3 py-2 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                                    >
                                      승인
                                    </button>
                                  </>
                              )}

                          {request.status ===
                              "OPEN" && (
                                  <button
                                      type="button"
                                      onClick={() =>
                                          setSelectedRequest(
                                              request
                                          )
                                      }
                                      className="rounded-lg border border-[#dce8e2] px-3 py-2 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                                  >
                                    상세 보기
                                  </button>
                              )}

                          {(request.status ===
                              "APPROVED" ||
                              request.status ===
                              "CLOSED") && (
                              <button
                                  type="button"
                                  onClick={() =>
                                      setSelectedRequest(
                                          request
                                      )
                                  }
                                  className="rounded-lg border border-[#dce8e2] px-3 py-2 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                              >
                                상세 보기
                              </button>
                          )}
                        </div>
                      </div>
                  )
              )}

              {filteredRequests.length ===
                  0 && (
                      <div className="flex min-h-[200px] items-center justify-center text-sm text-[#78847f]">
                        {tab === "WAITING" &&
                            "현재 수락을 기다리는 요청이 없습니다."}

                        {tab === "APPROVAL" &&
                            "현재 승인이 필요한 요청이 없습니다."}

                        {tab === "COMPLETED" &&
                            "처리 완료된 요청이 없습니다."}
                      </div>
                  )}
            </div>
          </div>
        </Card>

        <div className="mt-5 rounded-xl border border-[#dce8e2] bg-[#f3fbf7] p-4">
          <p className="text-sm font-black text-[#005642]">
            최종 승인 후
          </p>

          <p className="mt-1 text-sm leading-6 text-[#66736d]">
            대체 근무가 확정되고 실제 근무
            담당자가 수락한 직원으로
            변경됩니다.
          </p>
        </div>

        {/*
        TODO: 실제 SUB-06 API는 OPEN / ACCEPTED 상태의
        진행 중 요청만 조회한다.

        현재 "처리 완료" 탭은 프론트 UI 확인을 위한
        임시 mock이다.

        APPROVED / CLOSED 이력 조회 API가 별도로 확정되면
        해당 API 결과로 교체한다.
      */}

        {/* 상세 모달 */}
        {selectedRequest && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-black">
                      대체 근무 요청 상세
                    </h2>

                    <p className="mt-1 text-sm text-[#78847f]">
                      요청 상태와 근무 정보를
                      확인하세요.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={() =>
                          setSelectedRequest(
                              null
                          )
                      }
                      className="text-xl text-[#78847f] hover:text-black"
                  >
                    ×
                  </button>
                </div>

                <div className="mt-6 space-y-4">
                  <div className="rounded-xl bg-[#f3fbf7] p-4">
                    <p className="text-xs font-semibold text-[#78847f]">
                      근무 일시
                    </p>

                    <p className="mt-1 font-black">
                      {
                        selectedRequest.date
                      }{" "}
                      (
                      {
                        selectedRequest.day
                      }
                      )
                    </p>

                    <p className="mt-1 text-sm text-[#66736d]">
                      {
                        selectedRequest.startTime
                      }{" "}
                      ~{" "}
                      {
                        selectedRequest.endTime
                      }
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="rounded-xl border border-[#dce8e2] p-4">
                      <p className="text-xs text-[#78847f]">
                        요청자
                      </p>

                      <p className="mt-1 font-black">
                        {
                          selectedRequest.requesterName
                        }
                      </p>
                    </div>

                    <div className="rounded-xl border border-[#dce8e2] p-4">
                      <p className="text-xs text-[#78847f]">
                        수락자
                      </p>

                      <p className="mt-1 font-black">
                        {selectedRequest.acceptedCandidateName ??
                            "아직 없음"}
                      </p>
                    </div>
                  </div>

                  <div>
                    <p className="mb-2 text-sm font-bold">
                      현재 상태
                    </p>

                    {selectedRequest.status ===
                        "OPEN" && (
                            <div className="rounded-xl bg-[#eef4ff] px-4 py-3 text-sm font-bold text-[#4a73c9]">
                              후보 직원의 응답을
                              기다리고 있습니다.
                            </div>
                        )}

                    {selectedRequest.status ===
                        "ACCEPTED" && (
                            <div className="rounded-xl bg-[#fff8e8] px-4 py-3 text-sm font-bold text-[#a96d09]">
                              후보가 수락했습니다.
                              관리자 최종 승인이
                              필요합니다.
                            </div>
                        )}

                    {selectedRequest.status ===
                        "APPROVED" && (
                            <div className="rounded-xl bg-[#dff7ec] px-4 py-3 text-sm font-bold text-[#14956c]">
                              관리자 최종 승인이
                              완료되었습니다.
                            </div>
                        )}

                    {selectedRequest.status ===
                        "CLOSED" && (
                            <div className="rounded-xl bg-[#f1f3f2] px-4 py-3 text-sm font-bold text-[#66736d]">
                              종료된 요청입니다.
                            </div>
                        )}
                  </div>
                </div>

                <div className="mt-6 flex gap-3">
                  <button
                      type="button"
                      onClick={() =>
                          setSelectedRequest(
                              null
                          )
                      }
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    닫기
                  </button>

                  {selectedRequest.status ===
                      "OPEN" && (
                          <button
                              type="button"
                              onClick={() =>
                                  handleClose(
                                      selectedRequest.id
                                  )
                              }
                              className="flex-1 rounded-xl border border-[#f1cccc] px-4 py-3 text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5]"
                          >
                            요청 종료
                          </button>
                      )}

                  {selectedRequest.status ===
                      "ACCEPTED" && (
                          <button
                              type="button"
                              onClick={() =>
                                  handleApprove(
                                      selectedRequest.id
                                  )
                              }
                              className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                          >
                            최종 승인
                          </button>
                      )}
                </div>
              </div>
            </div>
        )}
      </>
  );
}