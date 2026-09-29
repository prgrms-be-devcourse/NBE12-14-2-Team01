"use client";

import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  ApiError,
  apiFetch,
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
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [status, setStatus] = useState<CurrentUserStatus>("loading");

  // 로그인한 사람 정보 한 번만 불러옴
  useEffect(() => {
    async function loadCurrentUser() {
      // 토큰이 없으면 부를 필요 없음
      if (!getToken()) {
        setStatus("error");
        return;
      }

      try {
        const data = await apiFetch<CurrentUser>("/auth/me");
        setUser(data);
        setStatus("success");
      } catch (error) {
        // 토큰 만료, 서버 꺼짐 등은 이름 자리만 비움
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
  }, []);

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
