"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  ApiError,
  apiFetch,
  setToken,
  UnexpectedResponseError,
} from "@/lib/api";

// 로그인 성공 시 서버가 data에 담아주는 값
type LoginResponse = {
  user: {
    id: number;
    email: string;
    name: string;
  };
  accessToken: string;
};

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage("");
    setIsLoading(true);

    try {
      const data = await apiFetch<LoginResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      setToken(data.accessToken);
      router.push("/workplaces");
    } catch (error) {
      if (error instanceof ApiError) {
        // 서버가 준 실패 메시지를 그대로 보여줌
        setErrorMessage(error.message);
      } else if (
          error instanceof UnexpectedResponseError ||
          error instanceof TypeError
      ) {
        // 서버 다운, 네트워크 끊김, 서버가 이상한 응답을 준 경우
        setErrorMessage("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
      } else {
        // 그 외 예상 못한 문제는 원인 확인용으로 콘솔에 남김
        console.error(error);
        setErrorMessage("로그인 중 문제가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      // 성공이든 실패든 끝나면 버튼 다시 풀어줌
      setIsLoading(false);
    }
  };

  return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5faf7] px-4">
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
                  autoComplete="current-password"
                  placeholder="비밀번호를 입력해주세요."
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
              {isLoading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-[#78847f]">
            아직 계정이 없으신가요?

            <Link
                href="/signup"
                className="ml-2 font-bold text-[#005642] hover:underline"
            >
              회원가입
            </Link>
          </div>
        </div>
      </main>
  );
}