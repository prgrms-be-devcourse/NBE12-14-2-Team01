"use client";

import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import AppHeader from "@/components/layout/AppHeader";
import AppSidebar from "@/components/layout/AppSidebar";
import { apiFetch } from "@/lib/api";
import type { WorkplaceRole } from "@/lib/navigation";

type Props = {
  children: ReactNode;
};

type MyWorkplaceResponse = {
  workplaceId: number;
  name: string;
  role: WorkplaceRole;
};

export default function WorkplaceLayout({ children }: Props) {
  const params = useParams();
  const workplaceId = params.workplaceId as string;

  const [role, setRole] = useState<WorkplaceRole | null>(null);

  useEffect(() => {
    const fetchRole = async () => {
      try {
        const workplaces = await apiFetch<MyWorkplaceResponse[]>("/workplaces");

        const currentWorkplace = workplaces.find(
            (workplace) => workplace.workplaceId === Number(workplaceId)
        );

        setRole(currentWorkplace?.role ?? null);
      } catch (error) {
        console.error(error);
      }
    };

    fetchRole();
  }, [workplaceId]);

  return (
      <div className="min-h-screen bg-[#f5faf7]">
        {role && (
            <AppSidebar
                workplaceId={workplaceId}
                role={role}
            />
        )}

        <div className="lg:pl-64">
          <AppHeader />

          <main className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-8">
            {children}
          </main>
        </div>
      </div>
  );
}