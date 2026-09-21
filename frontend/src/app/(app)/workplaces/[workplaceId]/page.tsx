"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";
import InviteCodeBox from "@/components/ui/InviteCodeBox";

type MemberRole = "MANAGER" | "EMPLOYEE";

type Member = {
  id: number;
  name: string;
  email: string;
  role: MemberRole;
};

// TODO: API 연동 후 실제 구성원 데이터로 교체
const members: Member[] = [
  {
    id: 1,
    name: "김지연",
    email: "jiyeon@switch.com",
    role: "MANAGER",
  },
];

export default function WorkplacePage() {
  const params = useParams();
  const workplaceId = params.workplaceId as string;

  const managerCount = members.filter(
      (member) => member.role === "MANAGER"
  ).length;

  const employeeCount = members.filter(
      (member) => member.role === "EMPLOYEE"
  ).length;

  const hasEmployees = employeeCount > 0;

  return (
      <>
        <PageHeader
            title="카페 스위치"
            description="근무지의 일정과 구성원을 한눈에 확인하세요."
        />

        {/* 요약 카드 */}
        <div className="grid gap-4 md:grid-cols-3">
          <Link
              href={`/workplaces/${workplaceId}/members`}
              className="block"
          >
            <Card className="h-full cursor-pointer transition hover:-translate-y-0.5 hover:shadow-md">
              <p className="text-sm font-semibold text-[#78847f]">
                현재 구성원
              </p>

              <p className="mt-2 text-3xl font-black text-[#005642]">
                {members.length}명
              </p>

              <p className="mt-1 text-sm text-[#78847f]">
                관리자 {managerCount}명
                {employeeCount > 0 && ` · 직원 ${employeeCount}명`}
              </p>
            </Card>
          </Link>

          <Link
              href={`/workplaces/${workplaceId}/schedule`}
              className="block"
          >
            <Card className="h-full cursor-pointer transition hover:-translate-y-0.5 hover:shadow-md">
              <p className="text-sm font-semibold text-[#78847f]">
                이번 주 공식 근무
              </p>

              <p className="mt-2 text-3xl font-black text-[#005642]">
                0건
              </p>

              <p className="mt-1 text-sm text-[#78847f]">
                아직 공개된 근무표가 없어요.
              </p>
            </Card>
          </Link>

          <Link
              href={`/workplaces/${workplaceId}/substitutes/admin`}
              className="block"
          >
            <Card className="h-full cursor-pointer transition hover:-translate-y-0.5 hover:shadow-md">
              <p className="text-sm font-semibold text-[#78847f]">
                진행 중 대체 근무
              </p>

              <p className="mt-2 text-3xl font-black text-[#005642]">
                0건
              </p>

              <p className="mt-1 text-sm text-[#78847f]">
                현재 처리할 요청이 없어요.
              </p>
            </Card>
          </Link>
        </div>

        {/* 시작하기 */}
        <Card className="mt-6">
          <div className="mb-5">
            <h2 className="text-lg font-black">
              시작하기
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              근무표를 만들기 위한 기본 설정을 진행해보세요.
            </p>
          </div>

          <div className="space-y-3">
            <Link
                href={`/workplaces/${workplaceId}/members`}
                className="flex w-full items-center justify-between rounded-xl border border-[#dce8e2] p-4 text-left transition hover:bg-[#f3fbf7]"
            >
              <div className="flex items-center gap-4">
              <span className="text-2xl">
                👥
              </span>

                <div>
                  <p className="font-bold">
                    직원 초대
                  </p>

                  <p className="mt-0.5 text-sm text-[#78847f]">
                    초대 코드를 공유하고 구성원을 추가하세요.
                  </p>
                </div>
              </div>

              <span className="text-[#78847f]">
              ›
            </span>
            </Link>

            <Link
                href={`/workplaces/${workplaceId}/regular-shifts`}
                className="flex w-full items-center justify-between rounded-xl border border-[#dce8e2] p-4 text-left transition hover:bg-[#f3fbf7]"
            >
              <div className="flex items-center gap-4">
              <span className="text-2xl">
                🗓️
              </span>

                <div>
                  <p className="font-bold">
                    정기 근무 등록
                  </p>

                  <p className="mt-0.5 text-sm text-[#78847f]">
                    직원별 반복 근무 요일과 시간을 등록하세요.
                  </p>
                </div>
              </div>

              <span className="text-[#78847f]">
              ›
            </span>
            </Link>

            <Link
                href={`/workplaces/${workplaceId}/schedule`}
                className="flex w-full items-center justify-between rounded-xl border border-[#dce8e2] p-4 text-left transition hover:bg-[#f3fbf7]"
            >
              <div className="flex items-center gap-4">
              <span className="text-2xl">
                📅
              </span>

                <div>
                  <p className="font-bold">
                    주간 근무표 만들기
                  </p>

                  <p className="mt-0.5 text-sm text-[#78847f]">
                    정기 근무를 기준으로 이번 주 근무표를 만드세요.
                  </p>
                </div>
              </div>

              <span className="text-[#78847f]">
              ›
            </span>
            </Link>
          </div>

          {/*
          TODO:
          직원 등록, 정기 근무 등록, 근무표 생성 등
          초기 설정이 완료되면 "시작하기" 영역을
          "칸편 메뉴" 형태로 변경할 수 있다.

          현재는 프론트 기본 틀 제공을 위해
          초기 상태 화면으로 유지한다.
        */}
        </Card>

        {/* 구성원 */}
        <Card className="mt-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-black">
                구성원
              </h2>

              <p className="mt-1 text-sm text-[#78847f]">
                현재 근무지에 참여한 사용자
              </p>
            </div>

            <Link
                href={`/workplaces/${workplaceId}/members`}
                className="text-sm font-bold text-[#005642] hover:underline"
            >
              전체 보기
            </Link>
          </div>

          {/* 구성원 미리보기 */}
          <div className="mt-5 space-y-3">
            {members.map((member) => (
                <div
                    key={member.id}
                    className="flex items-center justify-between rounded-xl bg-[#f3fbf7] p-4"
                >
                  <div className="flex items-center gap-3">
                    <div className="grid h-10 w-10 place-items-center rounded-full bg-[#dff7ec] font-black text-[#005642]">
                      {member.name.slice(0, 1)}
                    </div>

                    <div>
                      <p className="font-black">
                        {member.name}
                      </p>

                      <p className="mt-0.5 text-xs text-[#78847f]">
                        {member.email}
                      </p>
                    </div>
                  </div>

                  <span
                      className={`rounded-full px-3 py-1 text-xs font-bold ${
                          member.role === "MANAGER"
                              ? "bg-[#ece7ff] text-[#7861c9]"
                              : "bg-[#dff7ec] text-[#14956c]"
                      }`}
                  >
                {member.role === "MANAGER" ? "관리자" : "직원"}
              </span>
                </div>
            ))}
          </div>

          {/* 직원이 아직 없을 때만 표시 */}
          {!hasEmployees && (
              <div className="mt-5 rounded-xl border border-dashed border-[#c9ded4] p-5 text-center">
                <div className="text-2xl">
                  📨
                </div>

                <p className="mt-3 font-black">
                  아직 참여한 직원이 없어요.
                </p>

                <p className="mt-1 text-sm text-[#78847f]">
                  초대 코드를 직원에게 전달해보세요.
                </p>

                <div className="mt-4">
                  {/*
                TODO:
                Workplace 초대 코드 조회 API 연동 후
                inviteCode 값을 전달한다.

                예:
                <InviteCodeBox inviteCode={workplace.inviteCode}/>
              */}
                  <InviteCodeBox inviteCode={"SW-1234"} />
                </div>
              </div>
          )}
        </Card>
      </>
  );
}