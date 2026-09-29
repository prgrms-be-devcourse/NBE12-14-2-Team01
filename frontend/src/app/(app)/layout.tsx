import type { ReactNode } from "react";

import CurrentUserProvider from "@/components/providers/CurrentUserProvider";

// 로그인 이후 화면 전체에서 로그인한 사람 정보를 같이 씀
export default function AppGroupLayout({ children }: { children: ReactNode }) {
  return (
      <CurrentUserProvider>
        {children}
      </CurrentUserProvider>
  );
}
