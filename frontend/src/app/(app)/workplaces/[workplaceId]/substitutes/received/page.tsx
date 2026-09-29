"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import PageHeader from "@/components/ui/PageHeader";
import { apiFetch } from "@/lib/api";

type RequestStatus = "PENDING" | "ACCEPTED" | "REJECTED";

type SubstituteRequest = {
  id: number;
  requesterName: string;
  workplaceName: string;
  date: string;
  day: string;
  startTime: string;
  endTime: string;
  status: RequestStatus;
};

type ReceivedSubstituteRequestResponse = {
  candidateId: number;
  requestId: number;
  shiftId: number;
  workplaceId: number;
  workplaceName: string;
  requesterMemberId: number;
  requesterName: string;
  startAt: string;
  endAt: string;
  requestStatus: "OPEN";
  candidateStatus: "PENDING";
  createdAt: string;
};

type AcceptedSubstituteRequestResponse = {
  candidateId: number;
  requestId: number;
  shiftId: number;
  workplaceId: number;
  workplaceName: string;
  requesterName: string;
  startAt: string;
  endAt: string;
  requestStatus: "ACCEPTED";
  candidateStatus: "ACCEPTED";
  respondedAt: string;
};


type Tab = "PENDING" | "ACCEPTED" | "REJECTED";

export default function ReceivedRequestsPage() {
  const params = useParams();
  const workplaceId = Number(params.workplaceId);

  const [requests, setRequests] = useState<SubstituteRequest[]>([]);
  const [tab, setTab] = useState<Tab>("PENDING");
  const [respondingCandidateId, setRespondingCandidateId] =
      useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const fetchRequests = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);

        const [receivedData, acceptedData] = await Promise.all([
          apiFetch<ReceivedSubstituteRequestResponse[]>(
              "/substitute-requests/received"
          ),
          apiFetch<AcceptedSubstituteRequestResponse[]>(
              "/substitute-requests/accepted"
          ),
        ]);

        const dayNames = ["일", "월", "화", "수", "목", "금", "토"];

        const receivedRequests: SubstituteRequest[] = receivedData
        .filter((request) => request.workplaceId === workplaceId)
        .map((request) => {
          const start = new Date(request.startAt);
          const end = new Date(request.endAt);

          return {
            id: request.candidateId,
            requesterName: request.requesterName,
            workplaceName: request.workplaceName,
            date: `${start.getMonth() + 1}월 ${start.getDate()}일`,
            day: dayNames[start.getDay()],
            startTime: start.toTimeString().slice(0, 5),
            endTime: end.toTimeString().slice(0, 5),
            status: "PENDING",
          };
        });

        const acceptedRequests: SubstituteRequest[] = acceptedData
        .filter((request) => request.workplaceId === workplaceId)
        .map((request) => {
          const start = new Date(request.startAt);
          const end = new Date(request.endAt);

          return {
            id: request.candidateId,
            requesterName: request.requesterName,
            workplaceName: request.workplaceName,
            date: `${start.getMonth() + 1}월 ${start.getDate()}일`,
            day: dayNames[start.getDay()],
            startTime: start.toTimeString().slice(0, 5),
            endTime: end.toTimeString().slice(0, 5),
            status: "ACCEPTED",
          };
        });

        setRequests([...receivedRequests, ...acceptedRequests]);
      } catch (error) {
        setLoadError(
            error instanceof Error
                ? error.message
                : "대타 요청을 불러오지 못했습니다."
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchRequests();
  }, [workplaceId]);

  const handleAccept = async (candidateId: number) => {
    try {
      setRespondingCandidateId(candidateId);

      await apiFetch(
          `/substitute-candidates/${candidateId}/response`,
          {
            method: "PATCH",
            body: JSON.stringify({
              decision: "ACCEPT",
            }),
          }
      );

      setRequests((prevRequests) =>
          prevRequests.map((request) =>
              request.id === candidateId
                  ? { ...request, status: "ACCEPTED" }
                  : request
          )
      );
    } catch (error) {
      const message =
          error instanceof Error
              ? error.message
              : "대타 요청 수락에 실패했습니다.";

      alert(message);
    } finally {
      setRespondingCandidateId(null);
    }
  };

  const handleReject = async (candidateId: number) => {
    try {
      setRespondingCandidateId(candidateId);

      await apiFetch(
          `/substitute-candidates/${candidateId}/response`,
          {
            method: "PATCH",
            body: JSON.stringify({
              decision: "REJECT",
            }),
          }
      );

      setRequests((prevRequests) =>
          prevRequests.map((request) =>
              request.id === candidateId
                  ? { ...request, status: "REJECTED" }
                  : request
          )
      );
    } catch (error) {
      const message =
          error instanceof Error
              ? error.message
              : "대타 요청 거절에 실패했습니다.";

      alert(message);
    } finally {
      setRespondingCandidateId(null);
    }
  };

  const filteredRequests = requests.filter(
      (request) => request.status === tab
  );

  const pendingCount = requests.filter(
      (request) => request.status === "PENDING"
  ).length;

  const acceptedCount = requests.filter(
      (request) => request.status === "ACCEPTED"
  ).length;

  const rejectedCount = requests.filter(
      (request) => request.status === "REJECTED"
  ).length;

  return (
      <>
        <PageHeader
            title="받은 요청"
            description="나에게 도착한 대체 근무 요청을 확인하고 응답하세요."
        />

        {/* 탭 */}
        <div className="mb-5 flex flex-wrap gap-2">
          <button
              type="button"
              onClick={() => setTab("PENDING")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "PENDING"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            대기 중 {pendingCount}
          </button>

          <button
              type="button"
              onClick={() => setTab("ACCEPTED")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "ACCEPTED"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            수락한 요청 {acceptedCount}
          </button>

          <button
              type="button"
              onClick={() => setTab("REJECTED")}
              className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                  tab === "REJECTED"
                      ? "bg-[#005642] text-white"
                      : "text-[#66736d] hover:bg-[#f3fbf7]"
              }`}
          >
            거절한 요청 {rejectedCount}
          </button>
        </div>

        {/* 요청 목록 */}
        <div className="space-y-4">

          {isLoading && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#dce8e2] bg-white text-sm text-[#78847f]">
                대타 요청을 불러오는 중입니다...
              </div>
          )}

          {!isLoading && loadError && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#f1cccc] bg-white text-sm text-[#d95555]">
                {loadError}
              </div>
          )}

          {!isLoading && !loadError && filteredRequests.map((request) => (
              <div
                  key={request.id}
                  className="rounded-2xl border border-[#dce8e2] bg-white p-5 shadow-sm"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-black">
                      {request.date} ({request.day})
                    </h2>

                    <p className="mt-1 font-bold text-[#005642]">
                      {request.startTime} - {request.endTime}
                    </p>
                  </div>

                  {request.status === "PENDING" && (
                      <span className="rounded-full bg-[#fff1d7] px-3 py-1 text-xs font-bold text-[#a96d09]">
                  대기 중
                </span>
                  )}

                  {request.status === "ACCEPTED" && (
                      <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                  수락함
                </span>
                  )}

                  {request.status === "REJECTED" && (
                      <span className="rounded-full bg-[#fff1f1] px-3 py-1 text-xs font-bold text-[#d95555]">
                  거절함
                </span>
                  )}
                </div>

                {/* 요청자 */}
                <div className="mt-5 rounded-xl bg-[#f3fbf7] p-4">
                  <p className="text-xs font-semibold text-[#78847f]">
                    요청자
                  </p>

                  <p className="mt-1 font-black">{request.requesterName}</p>

                  <p className="mt-1 text-sm text-[#78847f]">
                    {request.workplaceName}
                  </p>
                </div>

                {/* 대기 중일 때만 버튼 표시 */}
                {request.status === "PENDING" && (
                    <div className="mt-5 grid grid-cols-2 gap-3">
                      <button
                          type="button"
                          onClick={() => handleReject(request.id)}
                          disabled={respondingCandidateId === request.id}
                          className="rounded-xl border border-[#f1cccc] px-4 py-3 font-bold text-[#d95555] transition hover:bg-[#fff5f5] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {respondingCandidateId === request.id ? "처리 중..." : "거절하기"}
                      </button>

                      <button
                          type="button"
                          onClick={() => handleAccept(request.id)}
                          disabled={respondingCandidateId === request.id}
                          className="rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {respondingCandidateId === request.id ? "처리 중..." : "수락하기"}
                      </button>
                    </div>
                )}

                {request.status === "ACCEPTED" && (
                    <div className="mt-5 rounded-xl bg-[#f3fbf7] px-4 py-3 text-center text-sm font-bold text-[#14956c]">
                      대체 근무 요청을 수락했습니다.
                    </div>
                )}

                {request.status === "REJECTED" && (
                    <div className="mt-5 rounded-xl bg-[#fff7f7] px-4 py-3 text-center text-sm font-bold text-[#d95555]">
                      대체 근무 요청을 거절했습니다.
                    </div>
                )}
              </div>
          ))}

          {/* 비어있는 탭 */}
          {!isLoading && !loadError && filteredRequests.length === 0 && (
              <div className="flex min-h-[240px] items-center justify-center rounded-2xl border border-dashed border-[#dce8e2] bg-white text-sm text-[#78847f]">
                {tab === "PENDING" && "현재 대기 중인 요청이 없습니다."}
                {tab === "ACCEPTED" && "수락한 요청이 없습니다."}
                {tab === "REJECTED" && "거절한 요청이 없습니다."}
              </div>
          )}
        </div>
      </>
  );
}