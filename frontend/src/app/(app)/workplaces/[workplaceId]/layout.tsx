"use client";

import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import AppHeader from "@/components/layout/AppHeader";
import AppSidebar from "@/components/layout/AppSidebar";
import { apiFetch } from "@/lib/api";
import type { WorkplaceRole } from "@/lib/navigation";

type RoleStatus = "loading" | "success" | "error" | "not-found";

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

  const [roleState, setRoleState] = useState<{
    workplaceId: string;
    role: WorkplaceRole | null;
    status: RoleStatus;
  }>({
    workplaceId,
    role: null,
    status: "loading",
  });

  const isCurrentWorkplace = roleState.workplaceId === workplaceId;
  const role = isCurrentWorkplace ? roleState.role : null;
  const roleStatus = isCurrentWorkplace ? roleState.status : "loading";

  useEffect(() => {
    let cancelled = false;

    const fetchRole = async () => {
      try {
        const workplaces =
            await apiFetch<MyWorkplaceResponse[]>("/workplaces");

        if (cancelled) return;

        const currentWorkplace = workplaces.find(
            (workplace) =>
                workplace.workplaceId === Number(workplaceId)
        );

        if (!currentWorkplace) {
          setRoleState({
            workplaceId,
            role: null,
            status: "not-found",
          });
          return;
        }

        setRoleState({
          workplaceId,
          role: currentWorkplace.role,
          status: "success",
        });
      } catch (error) {
        if (cancelled) return;

        console.error(error);

        setRoleState({
          workplaceId,
          role: null,
          status: "error",
        });
      }
    };

    void fetchRole();

    return () => {
      cancelled = true;
    };
  }, [workplaceId]);

  return (
      <div className="min-h-screen bg-[#f5faf7]">
        {roleStatus === "success" && role && (
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