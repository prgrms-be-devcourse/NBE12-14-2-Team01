"use client"

import Image from "next/image";
import Link from "next/link";

export default function SignupPage() {
  return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5faf7] px-4 py-10">
        <div className="w-full max-w-md rounded-3xl border border-[#dce8e2] bg-white p-8 shadow-sm">
          <div className="text-center">
            <Image
                src="/switch-logo.png"
                alt="SWITCH"
                width={220}
                height={165}
                priority
                className="mx-auto h-auto w-[220px]"
            />
          </div>

          <div className="mt-8">
            <h2 className="text-xl font-black">
              회원가입
            </h2>

            <p className="mt-1 text-sm text-[#78847f]">
              SWITCH를 시작하기 위한 정보를 입력해주세요.
            </p>
          </div>

          <form className="mt-6 space-y-5">
            <div>
              <label className="mb-2 block text-sm font-bold">
                이름
              </label>

              <input
                  type="text"
                  placeholder="이름을 입력해주세요."
                  className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
              />
            </div>

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

            <div>
              <label className="mb-2 block text-sm font-bold">
                비밀번호 확인
              </label>

              <input
                  type="password"
                  placeholder="비밀번호를 다시 입력해주세요."
                  className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
              />
            </div>

            <button
                type="submit"
                className="w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52]"
            >
              회원가입
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-[#78847f]">
            이미 계정이 있으신가요?

            <Link
                href="/login"
                className="ml-2 font-bold text-[#005642] hover:underline"
            >
              로그인
            </Link>
          </div>
        </div>
      </main>
  );
}