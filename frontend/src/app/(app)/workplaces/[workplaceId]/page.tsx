"use client";

import Link from "next/link";
import { useState } from "react";
import { useParams } from "next/navigation";
import Card from "@/components/ui/Card";
import PageHeader from "@/components/ui/PageHeader";

export default function WorkplacePage() {
  const params = useParams<{ workplaceId: string }>();
  const workplaceId = params.workplaceId;

  const inviteCode = "SW-9284";

  const [showCopyToast, setShowCopyToast] = useState(false);

  const handleCopyInviteCode = async () => {
    await navigator.clipboard.writeText(inviteCode);

    setShowCopyToast(true);

    setTimeout(() => {
      setShowCopyToast(false);
    }, 2000);
  };

  return (
      <>
        {showCopyToast && (
            <div className="fixed right-6 bottom-6 z-50 rounded-xl bg-[#005642] px-5 py-3 text-sm font-bold text-white shadow-lg">
              초대 코드가 복사되었습니다!
            </div>
        )}

        <PageHeader
            title="카페 스위치"
            description="근무지의 일정과 구성원을 한눈에 확인하세요."
        />

        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <p className="text-sm font-semibold text-[#78847f]">
              현재 구성원
            </p>

            <p className="mt-2 text-3xl font-black text-[#005642]">
              1명
            </p>

            <p className="mt-1 text-sm text-[#78847f]">
              관리자 1명
            </p>
          </Card>

          <Card>
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

          <Card>
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
        </div>

        <div className="mt-6 grid gap-6 xl:grid-cols-[1.4fr_1fr]">
          <Card>
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
                  <span className="text-2xl">👥</span>

                  <div>
                    <p className="font-bold">
                      직원 초대
                    </p>

                    <p className="text-sm text-[#78847f]">
                      초대 코드를 공유하고 구성원을 추가하세요.
                    </p>
                  </div>
                </div>

                <span>›</span>
              </Link>

              <Link
                  href={`/workplaces/${workplaceId}/regular-shifts`}
                  className="flex w-full items-center justify-between rounded-xl border border-[#dce8e2] p-4 text-left transition hover:bg-[#f3fbf7]"
              >
                <div className="flex items-center gap-4">
                  <span className="text-2xl">🗓️</span>

                  <div>
                    <p className="font-bold">
                      정기 근무 등록
                    </p>

                    <p className="text-sm text-[#78847f]">
                      직원별 반복 근무 요일과 시간을 등록하세요.
                    </p>
                  </div>
                </div>

                <span>›</span>
              </Link>

              <Link
                  href={`/workplaces/${workplaceId}/schedule`}
                  className="flex w-full items-center justify-between rounded-xl border border-[#dce8e2] p-4 text-left transition hover:bg-[#f3fbf7]"
              >
                <div className="flex items-center gap-4">
                  <span className="text-2xl">📅</span>

                  <div>
                    <p className="font-bold">
                      주간 근무표 만들기
                    </p>

                    <p className="text-sm text-[#78847f]">
                      정기 근무를 기준으로 이번 주 근무표를 만드세요.
                    </p>
                  </div>
                </div>

                <span>›</span>
              </Link>
            </div>
          </Card>

          <Card>
            <div className="flex items-center justify-between">
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
                  className="text-sm font-bold text-[#0b6b52]"
              >
                전체 보기
              </Link>
            </div>

            <div className="mt-5 flex items-center justify-between rounded-xl bg-[#f3fbf7] p-4">
              <div className="flex items-center gap-3">
                <div className="grid h-10 w-10 place-items-center rounded-full bg-[#dff7ec] font-bold text-[#005642]">
                  김
                </div>

                <div>
                  <p className="font-bold">
                    김지연
                  </p>

                  <p className="text-xs text-[#78847f]">
                    jiyeon@switch.com
                  </p>
                </div>
              </div>

              <span className="rounded-full bg-[#eae5ff] px-3 py-1 text-xs font-bold text-[#6758c7]">
              관리자
            </span>
            </div>

            <div className="mt-5 rounded-xl border border-dashed border-[#c9ded4] p-5 text-center">
              <div className="text-3xl">
                📨
              </div>

              <p className="mt-2 font-bold">
                아직 참여한 직원이 없어요.
              </p>

              <p className="mt-1 text-sm text-[#78847f]">
                초대 코드를 직원에게 전달해보세요.
              </p>

              <div className="mt-4 rounded-xl bg-[#f3fbf7] p-3">
                <p className="text-xs text-[#78847f]">
                  초대 코드
                </p>

                <p className="mt-1 text-lg font-black tracking-wider text-[#005642]">
                  {inviteCode}
                </p>
              </div>

              <button
                  type="button"
                  onClick={handleCopyInviteCode}
                  className="mt-4 w-full rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
              >
                초대 코드 복사
              </button>
            </div>
          </Card>
        </div>
      </>
  );
}