"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const router = useRouter();

  const handleLogin = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 임시 로그인 처리
    router.push("/workplaces");
  };

  return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5faf7] px-4">
        <div className="w-full max-w-md rounded-3xl border border-[#dce8e2] bg-white p-8 shadow-sm">
          <div className="text-center">
            <h1 className="text-3xl font-black text-[#005642]">
              SWITCH
            </h1>

            <p className="mt-2 text-sm text-[#78847f]">
              근무가 필요한 순간, 더 유연한 하루
            </p>
          </div>

          <div className="mt-8">
            <h2 className="text-xl font-black">
              로그인
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              계정 정보를 입력해주세요.
            </p>
          </div>

          <form
              onSubmit={handleLogin}
              className="mt-6 space-y-5"
          >
            <div>
              <label className="mb-2 block text-sm font-bold">
                이메일
              </label>

              <input
                  type="email"
                  placeholder="example@switch.com"
                  className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
              />
            </div>

            <div>
              <label className="mb-2 block text-sm font-bold">
                비밀번호
              </label>

              <input
                  type="password"
                  placeholder="비밀번호를 입력해주세요."
                  className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
              />
            </div>

            <button
                type="submit"
                className="w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52]"
            >
              로그인
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-[#78847f]">
            아직 계정이 없으신가요?

            <a
                href="/signup"
                className="ml-2 font-bold text-[#005642] hover:underline"
            >
              회원가입
            </a>
          </div>
        </div>
      </main>
  );
}