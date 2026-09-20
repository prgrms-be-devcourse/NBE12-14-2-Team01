import AppHeader from "@/components/layout/AppHeader";
import AppSidebar from "@/components/layout/AppSidebar";
import MobileBottomNav from "@/components/layout/MobileBottomNav";

type Props = {
  children: React.ReactNode;
  params: Promise<{
    workplaceId: string;
  }>;
};

export default async function WorkplaceLayout({
                                                children,
                                                params,
                                              }: Props) {
  const { workplaceId } = await params;

  // TODO API 연결 후 실제 사용자/근무지 정보로 변경
  const workplaceName = "카페 스위치";
  const userName = "김지연";
  const role = "MANAGER" as const;

  return (
      <div className="min-h-screen bg-[#f5faf7]">
        <AppSidebar
            workplaceId={workplaceId}
            role={role}
        />

        <div className="lg:pl-64">
          <AppHeader
              workplaceName={workplaceName}
              userName={userName}
              role={role}
          />

          <main className="mx-auto w-full max-w-[1440px] px-4 py-6 pb-24 sm:px-6 lg:px-8 lg:pb-8">
            {children}
          </main>
        </div>

        <MobileBottomNav
            workplaceId={workplaceId}
            role={role}
        />
      </div>
  );
}