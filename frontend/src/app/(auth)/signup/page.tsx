"use client"

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError, apiFetch, UnexpectedResponseError } from "@/lib/api";

// 회원가입 성공 시 서버가 data에 담아주는 값 (토큰은 없음)
type SignupResponse = {
  id: number;
  email: string;
  name: string;
};

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSignup = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage("");

    // 서버가 막는 빈칸을 먼저 알기 쉽게 알려줌
    if (!name.trim()) {
      setErrorMessage("이름을 입력해주세요.");
      return;
    }

    if (!password.trim()) {
      setErrorMessage("비밀번호를 입력해주세요.");
      return;
    }

    // 비밀번호 두 개가 같은지 먼저 확인
    if (password !== passwordConfirm) {
      setErrorMessage("비밀번호가 일치하지 않아요.");
      return;
    }

    setIsLoading(true);

    try {
      await apiFetch<SignupResponse>("/auth/signup", {
        method: "POST",
        // 비밀번호는 공백도 그대로 보냄
        body: JSON.stringify({
          email: email.trim(),
          password,
          name: name.trim(),
        }),
      });

      router.push("/login?signup=success");
    } catch (error) {
      if (error instanceof ApiError) {
        // 서버가 준 실패 메시지를 그대로 보여줌 (예: 이미 사용중인 이메일)
        setErrorMessage(error.message);
      } else if (
          error instanceof UnexpectedResponseError ||
          error instanceof TypeError
      ) {
        setErrorMessage("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
      } else {
        console.error(error);
        setErrorMessage("회원가입 중 문제가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      setIsLoading(false);
    }
  };

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

          <form
              onSubmit={handleSignup}
              className="mt-6 space-y-5"
          >
            <div>
              <label className="mb-2 block text-sm font-bold">
                이름
              </label>

              <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoComplete="name"
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
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
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
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
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
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  required
                  autoComplete="new-password"
                  placeholder="비밀번호를 다시 입력해주세요."
                  className="w-full rounded-xl border border-[#dce8e2] px-4 py-3 outline-none transition focus:border-[#14956c]"
              />
            </div>

            {errorMessage && (
                <p className="text-sm font-bold text-[#d95555]">
                  {errorMessage}
                </p>
            )}

            <button
                type="submit"
                disabled={isLoading}
                className="w-full rounded-xl bg-[#005642] px-4 py-3 font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoading ? "가입 중..." : "회원가입"}
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