"use client";

import Link from "next/link";
import { useState } from "react";

const initialWorkplaces  = [
  {
    id: 1,
    name: "카페 스위치",
    role: "MANAGER",
    members: 4,
  },
  {
    id: 2,
    name: "스위치 베이커리",
    role: "EMPLOYEE",
    members: 7,
  },
];

export default function WorkplacesPage() {
  const [workplaces, setWorkplaces] = useState(initialWorkplaces);
  const [workplaceName, setWorkplaceName] = useState("");

  const handleCreateWorkplace = () => {
    const name = workplaceName.trim();

    if (!name) {
      alert("근무지 이름을 입력해주세요.");
      return;
    }

    const newWorkplace = {
      id: workplaces.length + 1,
      name,
      role: "MANAGER",
      members: 1,
    };

    setWorkplaces([...workplaces, newWorkplace]);
    setWorkplaceName("");
  };
  return (
      <main className="min-h-screen bg-[#f5faf7] px-4 py-10">
        <div className="mx-auto max-w-5xl">
          <div className="mb-8">
            <h1 className="text-3xl font-black text-[#005642]">
              SWITCH
            </h1>

            <p className="mt-2 text-[#78847f]">
              참여할 근무지를 선택하거나 새로운 근무지를 시작해보세요.
            </p>
          </div>

          <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
            {/* 내 근무지 */}
            <section>
              <div className="mb-4 flex items-end justify-between">
                <div>
                  <h2 className="text-xl font-black">
                    내 근무지
                  </h2>

                  <p className="mt-1 text-sm text-[#78847f]">
                    현재 참여 중인 근무지입니다.
                  </p>
                </div>

                <span className="text-sm font-bold text-[#005642]">
                {workplaces.length}개
              </span>
              </div>

              <div className="space-y-3">
                {workplaces.map((workplace) => (
                    <Link
                        key={workplace.id}
                        href={`/workplaces/${workplace.id}`}
                        className="block rounded-2xl border border-[#dce8e2] bg-white p-5 transition hover:border-[#14956c] hover:shadow-sm"
                    >
                      <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-4">
                          <div className="grid h-12 w-12 place-items-center rounded-xl bg-[#dff7ec] text-xl font-black text-[#005642]">
                            {workplace.name.slice(0, 1)}
                          </div>

                          <div>
                            <p className="font-black">
                              {workplace.name}
                            </p>

                            <p className="mt-1 text-sm text-[#78847f]">
                              구성원 {workplace.members}명
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center gap-3">
                      <span
                          className={
                            workplace.role === "MANAGER"
                                ? "rounded-full bg-[#ece8ff] px-3 py-1 text-xs font-bold text-[#6758c7]"
                                : "rounded-full bg-[#f3f5f4] px-3 py-1 text-xs font-bold text-[#66736d]"
                          }
                      >
                        {workplace.role === "MANAGER"
                            ? "관리자"
                            : "직원"}
                      </span>

                          <span className="text-xl text-[#9eaaa5]">
                        ›
                      </span>
                        </div>
                      </div>
                    </Link>
                ))}
              </div>
            </section>

            {/* 근무지 추가 */}
            <section className="space-y-4">
              <div className="rounded-2xl border border-[#dce8e2] bg-white p-6">
                <h2 className="text-lg font-black">
                  새 근무지 만들기
                </h2>

                <p className="mt-1 text-sm leading-6 text-[#78847f]">
                  새로운 근무지를 만들면 관리자로 시작합니다.
                </p>

                <div className="mt-5">
                  <label className="mb-2 block text-sm font-bold">
                    근무지 이름
                  </label>

                  <input
                      type="text"
                      value={workplaceName}
                      onChange={(e) => setWorkplaceName(e.target.value)}
                      placeholder="예) SWITCH 카페"
                      className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
                  />
                </div>

                <button
                    type="button"
                    onClick={handleCreateWorkplace}
                    className="mt-4 w-full rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52]"
                >
                  근무지 만들기
                </button>
              </div>

              <div className="rounded-2xl border border-[#dce8e2] bg-white p-6">
                <h2 className="text-lg font-black">
                  초대 코드로 참여
                </h2>

                <p className="mt-1 text-sm leading-6 text-[#78847f]">
                  관리자에게 받은 초대 코드를 입력해주세요.
                </p>

                <div className="mt-5">
                  <label className="mb-2 block text-sm font-bold">
                    초대 코드
                  </label>

                  <input
                      type="text"
                      placeholder="예) SW-9284"
                      className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 font-bold uppercase tracking-wider outline-none transition focus:border-[#14956c]"
                  />
                </div>

                <button
                    type="button"
                    className="mt-4 w-full rounded-xl border border-[#005642] bg-white px-4 py-3 text-sm font-bold text-[#005642] transition hover:bg-[#f3fbf7]"
                >
                  근무지 참여하기
                </button>
              </div>
            </section>
          </div>

          <div className="mt-8 text-center">
            <Link
                href="/login"
                className="text-sm font-semibold text-[#78847f] hover:text-[#005642]"
            >
              로그아웃
            </Link>
          </div>
        </div>
      </main>
  );
}