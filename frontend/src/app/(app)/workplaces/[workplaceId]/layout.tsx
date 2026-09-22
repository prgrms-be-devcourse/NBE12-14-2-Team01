import type { ReactNode } from "react";

import AppHeader from "@/components/layout/AppHeader";
import AppSidebar from "@/components/layout/AppSidebar";

type Props = {
  children: ReactNode;
  params: Promise<{
    workplaceId: string;
  }>;
};

export default async function WorkplaceLayout({ children, params }: Props) {
  const { workplaceId } = await params;

  // TODO: API 연결 후 실제 사용자 역할로 변경
  const role = "MANAGER" as const;

  return (
      <div className="min-h-screen bg-[#f5faf7]">
        <AppSidebar
            workplaceId={workplaceId}
            role={role}
        />

        <div className="lg:pl-64">
          <AppHeader />

          <main className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-8">
            {children}
          </main>
        </div>
      </div>
  );
}