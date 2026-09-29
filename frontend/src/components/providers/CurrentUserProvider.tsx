"use client";

import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import {
  ApiError,
  apiFetch,
  clearToken,
  getToken,
  UnexpectedResponseError,
} from "@/lib/api";

// 로그인한 사람 정보 (GET /auth/me 응답)
export type CurrentUser = {
  id: number;
  email: string;
  name: string;
};

export type CurrentUserStatus = "loading" | "success" | "error";

type CurrentUserContextValue = {
  user: CurrentUser | null;
  status: CurrentUserStatus;
};

const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

export default function CurrentUserProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [status, setStatus] = useState<CurrentUserStatus>("loading");

  // 로그인한 사람 정보 한 번만 불러옴
  useEffect(() => {
    async function loadCurrentUser() {
      // 로그인 안 했으면 로그인 화면으로
      if (!getToken()) {
        router.replace("/login");
        return;
      }

      try {
        const data = await apiFetch<CurrentUser>("/auth/me");
        setUser(data);
        setStatus("success");
      } catch (error) {
        // 토큰이 틀렸거나 재발급도 실패하면 토큰 지우고 로그인 화면으로
        if (error instanceof ApiError && error.status === 401) {
          clearToken();
          router.replace("/login");
          return;
        }

        // 서버 꺼짐 등은 로그인 화면으로 안 보내고 이름 자리만 비움
        if (
            !(error instanceof ApiError) &&
            !(error instanceof UnexpectedResponseError) &&
            !(error instanceof TypeError)
        ) {
          console.error(error);
        }

        setUser(null);
        setStatus("error");
      }
    }

    loadCurrentUser();
  }, [router]);

  // 로그인 확인 끝날 때까지 화면 안 그림
  if (status === "loading") {
    return null;
  }

  return (
      <CurrentUserContext.Provider value={{ user, status }}>
        {children}
      </CurrentUserContext.Provider>
  );
}

// 로그인한 사람 정보 꺼내 쓰기
export function useCurrentUser(): CurrentUserContextValue {
  const value = useContext(CurrentUserContext);

  if (!value) {
    throw new Error("useCurrentUser는 CurrentUserProvider 안에서만 쓸 수 있어요.");
  }

  return value;
}
