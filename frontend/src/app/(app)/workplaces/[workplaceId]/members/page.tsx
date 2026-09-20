"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import InviteCodeBox from "@/components/ui/InviteCodeBox";

const members = [
  {
    id: 1,
    name: "김지연",
    email: "jiyeon@switch.com",
    role: "MANAGER",
  },
  {
    id: 2,
    name: "이서연",
    email: "seoyeon@switch.com",
    role: "EMPLOYEE",
  },
  {
    id: 3,
    name: "박민수",
    email: "minsu@switch.com",
    role: "EMPLOYEE",
  },
  {
    id: 4,
    name: "최하은",
    email: "haeun@switch.com",
    role: "EMPLOYEE",
  },
];

export default function MembersPage() {

  const [showInviteModal, setShowInviteModal] = useState(false);

  const [filter, setFilter] = useState<
      "ALL" | "MANAGER" | "EMPLOYEE"
  >("ALL");

  const filteredMembers = members.filter((member) => {
    if (filter === "ALL") {
      return true;
    }

    return member.role === filter;
  });

  return (
      <>
        <PageHeader
            title="직원 목록"
            description="현재 근무지에 참여한 구성원을 확인하세요."
        >
          <button
              type="button"
              onClick={() => setShowInviteModal(true)}
              className="rounded-xl bg-[#005642] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
          >
            + 직원 초대
          </button>
        </PageHeader>

        <Card>
          <div className="flex gap-2">
            <button
                type="button"
                onClick={() => setFilter("ALL")}
                className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                    filter === "ALL"
                        ? "bg-[#005642] text-white"
                        : "bg-[#f3fbf7] text-[#66736d] hover:bg-[#dff7ec]"
                }`}
            >
              전체 {members.length}
            </button>

            <button
                type="button"
                onClick={() => setFilter("MANAGER")}
                className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                    filter === "MANAGER"
                        ? "bg-[#005642] text-white"
                        : "bg-[#f3fbf7] text-[#66736d] hover:bg-[#dff7ec]"
                }`}
            >
              관리자{" "}
              {members.filter((member) => member.role === "MANAGER").length}
            </button>

            <button
                type="button"
                onClick={() => setFilter("EMPLOYEE")}
                className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                    filter === "EMPLOYEE"
                        ? "bg-[#005642] text-white"
                        : "bg-[#f3fbf7] text-[#66736d] hover:bg-[#dff7ec]"
                }`}
            >
              직원{" "}
              {members.filter((member) => member.role === "EMPLOYEE").length}
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[650px] border-collapse">
              <thead>
              <tr className="border-b border-[#dce8e2] text-left text-sm text-[#78847f]">
                <th className="px-3 py-3">이름</th>
                <th className="px-3 py-3">이메일</th>
                <th className="px-3 py-3">역할</th>
                <th className="px-3 py-3">상태</th>
              </tr>
              </thead>

              <tbody>
              {filteredMembers.map((member) => (
                  <tr
                      key={member.id}
                      className="border-b border-[#edf2ef] last:border-none"
                  >
                    <td className="px-3 py-4">
                      <div className="flex items-center gap-3">
                        <div className="grid h-10 w-10 place-items-center rounded-full bg-[#dff7ec] font-bold text-[#005642]">
                          {member.name.slice(0, 1)}
                        </div>

                        <span className="font-bold">
                        {member.name}
                      </span>
                      </div>
                    </td>

                    <td className="px-3 py-4 text-sm text-[#66736d]">
                      {member.email}
                    </td>

                    <td className="px-3 py-4">
                    <span
                        className={
                          member.role === "MANAGER"
                              ? "rounded-full bg-[#ece8ff] px-3 py-1 text-xs font-bold text-[#6758c7]"
                              : "rounded-full bg-[#f3f5f4] px-3 py-1 text-xs font-bold text-[#66736d]"
                        }
                    >
                      {member.role === "MANAGER"
                          ? "관리자"
                          : "직원"}
                    </span>
                    </td>

                    <td className="px-3 py-4">
                    <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                      활성
                    </span>
                    </td>
                  </tr>
              ))}
              </tbody>
            </table>
          </div>
        </Card>
        {showInviteModal && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-black">
                      직원 초대
                    </h2>

                    <p className="mt-1 text-sm text-[#78847f]">
                      아래 초대 코드를 직원에게 공유해주세요.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={() => setShowInviteModal(false)}
                      className="text-xl text-[#78847f] hover:text-black"
                  >
                    ×
                  </button>
                </div>

                <div className="mt-6">
                  {/*초대코드 값 안넣으면 [초대 코드를 불러오는 중입니다.] 문구 */}
                  <InviteCodeBox inviteCode="SW-9284" />
                </div>

                <p className="mt-4 text-sm leading-6 text-[#78847f]">
                  직원은 로그인 후 근무지 참여 화면에서
                  이 코드를 입력하면 근무지에 참여할 수 있어요.
                </p>

                <button
                    type="button"
                    onClick={() => setShowInviteModal(false)}
                    className="mt-4 w-full rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] hover:bg-[#f3fbf7]"
                >
                  닫기
                </button>
              </div>
            </div>
        )}
      </>
  );
}