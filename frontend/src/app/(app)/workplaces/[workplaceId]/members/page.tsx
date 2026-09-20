"use client";

import { useState } from "react";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

type MemberRole =
    | "MANAGER"
    | "EMPLOYEE";

type Member = {
  id: number;
  name: string;
  email: string;
  role: MemberRole;

  // null이면 현재 소속
  // 값이 있으면 소속 종료
  leftAt: string | null;
};

const initialMembers: Member[] = [
  {
    id: 1,
    name: "김지연",
    email: "jiyeon@switch.com",
    role: "MANAGER",
    leftAt: null,
  },
  {
    id: 2,
    name: "이서연",
    email: "seoyeon@switch.com",
    role: "EMPLOYEE",
    leftAt: null,
  },
  {
    id: 3,
    name: "박민수",
    email: "minsu@switch.com",
    role: "EMPLOYEE",
    leftAt: null,
  },
  {
    id: 4,
    name: "최하은",
    email: "haeun@switch.com",
    role: "EMPLOYEE",
    leftAt: null,
  },
];

export default function MembersPage() {
  const [members, setMembers] =
      useState<Member[]>(
          initialMembers
      );

  const [filter, setFilter] = useState<
      "ALL" | "MANAGER" | "EMPLOYEE"
  >("ALL");

  const [
    showInviteModal,
    setShowInviteModal,
  ] = useState(false);

  const [
    showCopyToast,
    setShowCopyToast,
  ] = useState(false);

  const [
    openMenuId,
    setOpenMenuId,
  ] = useState<number | null>(null);

  const [
    memberToEnd,
    setMemberToEnd,
  ] = useState<Member | null>(null);

  const [
    successMessage,
    setSuccessMessage,
  ] = useState("");

  /*
   * TODO: API 연동 후
   * WP-05 초대 코드 조회 결과로 교체
   */
  const inviteCode = "SW-9284";

  const filteredMembers =
      members.filter((member) => {
        if (filter === "ALL") {
          return true;
        }

        return member.role === filter;
      });

  const managerCount =
      members.filter(
          (member) =>
              member.role === "MANAGER"
      ).length;

  const employeeCount =
      members.filter(
          (member) =>
              member.role === "EMPLOYEE"
      ).length;

  const handleCopyInviteCode =
      async () => {
        await navigator.clipboard.writeText(
            inviteCode
        );

        setShowCopyToast(true);

        setTimeout(() => {
          setShowCopyToast(false);
        }, 2000);
      };

  const handleEndMembership = () => {
    if (!memberToEnd) {
      return;
    }

    const endedMemberName =
        memberToEnd.name;

    /*
     * TODO: P1 소속 종료 API 연동
     *
     * 실제 구현에서는 회원 자체를 삭제하지 않고
     * 해당 WorkplaceMember.leftAt을
     * 종료 시각으로 변경한다.
     *
     * 소속 종료 시 현재 RegularShiftPattern은
     * 정리해야 한다.
     *
     * 이미 생성된 Shift는 자동 삭제하거나
     * 다른 직원에게 자동 재배정하지 않는다.
     */
    setMembers((prev) =>
        prev.map((member) =>
            member.id === memberToEnd.id
                ? {
                  ...member,
                  leftAt:
                      new Date().toISOString(),
                }
                : member
        )
    );

    setMemberToEnd(null);
    setOpenMenuId(null);

    setSuccessMessage(
        `${endedMemberName}님의 소속이 종료되었습니다.`
    );

    setTimeout(() => {
      setSuccessMessage("");
    }, 2500);
  };

  return (
      <>
        <PageHeader
            title="직원 목록"
            description="근무지 구성원과 소속 상태를 확인하세요."
        >
          <button
              type="button"
              onClick={() =>
                  setShowInviteModal(true)
              }
              className="rounded-xl bg-[#005642] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
          >
            + 직원 초대
          </button>
        </PageHeader>

        <Card>
          {/* 필터 */}
          <div className="flex gap-2">
            <button
                type="button"
                onClick={() =>
                    setFilter("ALL")
                }
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
                onClick={() =>
                    setFilter("MANAGER")
                }
                className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                    filter === "MANAGER"
                        ? "bg-[#005642] text-white"
                        : "bg-[#f3fbf7] text-[#66736d] hover:bg-[#dff7ec]"
                }`}
            >
              관리자 {managerCount}
            </button>

            <button
                type="button"
                onClick={() =>
                    setFilter("EMPLOYEE")
                }
                className={`rounded-full px-4 py-2 text-sm font-bold transition ${
                    filter === "EMPLOYEE"
                        ? "bg-[#005642] text-white"
                        : "bg-[#f3fbf7] text-[#66736d] hover:bg-[#dff7ec]"
                }`}
            >
              직원 {employeeCount}
            </button>
          </div>

          {/* 직원 목록 */}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse">
              <thead>
              <tr className="border-b border-[#dce8e2] text-left text-sm text-[#78847f]">
                <th className="px-3 py-3">
                  이름
                </th>

                <th className="px-3 py-3">
                  이메일
                </th>

                <th className="px-3 py-3">
                  역할
                </th>

                <th className="px-3 py-3">
                  상태
                </th>

                <th className="px-3 py-3 text-right">
                  관리
                </th>
              </tr>
              </thead>

              <tbody>
              {filteredMembers.map(
                  (member) => {
                    const isActive =
                        member.leftAt === null;

                    return (
                        <tr
                            key={member.id}
                            className={`border-b border-[#edf2ef] last:border-none ${
                                !isActive
                                    ? "bg-[#fafbfa]"
                                    : ""
                            }`}
                        >
                          {/* 이름 */}
                          <td className="px-3 py-4">
                            <div className="flex items-center gap-3">
                              <div
                                  className={`grid h-10 w-10 place-items-center rounded-full font-bold ${
                                      isActive
                                          ? "bg-[#dff7ec] text-[#005642]"
                                          : "bg-[#f0f2f1] text-[#9aa5a0]"
                                  }`}
                              >
                                {member.name.slice(
                                    0,
                                    1
                                )}
                              </div>

                              <span
                                  className={`font-bold ${
                                      !isActive
                                          ? "text-[#9aa5a0]"
                                          : ""
                                  }`}
                              >
                            {member.name}
                          </span>
                            </div>
                          </td>

                          {/* 이메일 */}
                          <td
                              className={`px-3 py-4 text-sm ${
                                  isActive
                                      ? "text-[#66736d]"
                                      : "text-[#a6afab]"
                              }`}
                          >
                            {member.email}
                          </td>

                          {/* 역할 */}
                          <td className="px-3 py-4">
                        <span
                            className={
                              member.role ===
                              "MANAGER"
                                  ? "rounded-full bg-[#ece8ff] px-3 py-1 text-xs font-bold text-[#6758c7]"
                                  : "rounded-full bg-[#f3f5f4] px-3 py-1 text-xs font-bold text-[#66736d]"
                            }
                        >
                          {member.role ===
                          "MANAGER"
                              ? "관리자"
                              : "직원"}
                        </span>
                          </td>

                          {/* 상태 */}
                          <td className="px-3 py-4">
                            {isActive ? (
                                <span className="rounded-full bg-[#dff7ec] px-3 py-1 text-xs font-bold text-[#14956c]">
                            활성
                          </span>
                            ) : (
                                <span className="rounded-full bg-[#f1f3f2] px-3 py-1 text-xs font-bold text-[#78847f]">
                            소속 종료
                          </span>
                            )}
                          </td>

                          {/* 관리 */}
                          <td className="px-3 py-4">
                            <div className="relative flex justify-end">
                              {member.role ===
                              "EMPLOYEE" &&
                              isActive ? (
                                  <>
                                    <button
                                        type="button"
                                        onClick={() =>
                                            setOpenMenuId(
                                                openMenuId ===
                                                member.id
                                                    ? null
                                                    : member.id
                                            )
                                        }
                                        className="rounded-lg px-3 py-2 text-xl leading-none text-[#78847f] transition hover:bg-[#f3fbf7] hover:text-[#005642]"
                                        aria-label={`${member.name} 관리 메뉴`}
                                    >
                                      ⋮
                                    </button>

                                    {openMenuId ===
                                        member.id && (
                                            <div className="absolute right-0 top-10 z-20 w-36 overflow-hidden rounded-xl border border-[#dce8e2] bg-white shadow-lg">
                                              <button
                                                  type="button"
                                                  onClick={() => {
                                                    setMemberToEnd(
                                                        member
                                                    );

                                                    setOpenMenuId(
                                                        null
                                                    );
                                                  }}
                                                  className="w-full px-4 py-3 text-left text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5]"
                                              >
                                                소속 종료
                                              </button>
                                            </div>
                                        )}
                                  </>
                              ) : (
                                  <span className="px-3 text-sm text-[#b0b8b4]">
                              -
                            </span>
                              )}
                            </div>
                          </td>
                        </tr>
                    );
                  }
              )}
              </tbody>
            </table>
          </div>
        </Card>

        {/* 소속 종료 확인 모달 */}
        {memberToEnd && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <h2 className="text-xl font-black">
                  소속을 종료할까요?
                </h2>

                <p className="mt-3 text-sm leading-6 text-[#66736d]">
                  <strong className="text-[#1f2925]">
                    {memberToEnd.name}
                  </strong>
                  님의 카페 스위치 소속을
                  종료합니다.
                </p>

                <div className="mt-5 rounded-xl bg-[#fff7f7] p-4">
                  <p className="text-sm font-bold text-[#d95555]">
                    계정 자체가 삭제되는 것은
                    아니에요.
                  </p>

                  <p className="mt-1 text-sm leading-6 text-[#78847f]">
                    해당 근무지와의 소속 관계만
                    종료됩니다.
                  </p>
                </div>

                <div className="mt-6 flex gap-3">
                  <button
                      type="button"
                      onClick={() =>
                          setMemberToEnd(null)
                      }
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] transition hover:bg-[#f3fbf7]"
                  >
                    취소
                  </button>

                  <button
                      type="button"
                      onClick={
                        handleEndMembership
                      }
                      className="flex-1 rounded-xl bg-[#d95555] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#c54848]"
                  >
                    소속 종료
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* 직원 초대 모달 */}
        {showInviteModal && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 px-4">
              <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-black">
                      직원 초대
                    </h2>

                    <p className="mt-1 text-sm text-[#78847f]">
                      아래 초대 코드를 직원에게
                      공유해주세요.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={() =>
                          setShowInviteModal(false)
                      }
                      className="text-xl text-[#78847f] hover:text-black"
                  >
                    ×
                  </button>
                </div>

                <div className="mt-6 rounded-xl bg-[#f3fbf7] p-5 text-center">
                  <p className="text-sm text-[#78847f]">
                    초대 코드
                  </p>

                  <p className="mt-2 text-2xl font-black tracking-widest text-[#005642]">
                    {inviteCode}
                  </p>
                </div>

                <p className="mt-4 text-sm leading-6 text-[#78847f]">
                  직원은 로그인 후 근무지 참여
                  화면에서 이 코드를 입력하면
                  근무지에 참여할 수 있어요.
                </p>

                <div className="mt-6 flex gap-3">
                  <button
                      type="button"
                      onClick={() =>
                          setShowInviteModal(false)
                      }
                      className="flex-1 rounded-xl border border-[#dce8e2] px-4 py-3 text-sm font-bold text-[#66736d] hover:bg-[#f3fbf7]"
                  >
                    닫기
                  </button>

                  <button
                      type="button"
                      onClick={
                        handleCopyInviteCode
                      }
                      className="flex-1 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white hover:bg-[#0b6b52]"
                  >
                    초대 코드 복사
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* 성공 토스트 */}
        {successMessage && (
            <div className="fixed bottom-6 right-6 z-50 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white shadow-lg">
              {successMessage}
            </div>
        )}

        {showCopyToast && (
            <div className="fixed bottom-6 right-6 z-50 rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white shadow-lg">
              초대 코드를 복사했어요.
            </div>
        )}
      </>
  );
}