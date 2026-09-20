"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { WorkplaceRole } from "@/lib/navigation";

type Props = {
  workplaceId: string;
  role: WorkplaceRole;
};

export default function MobileBottomNav({
                                          workplaceId,
                                          role,
                                        }: Props) {
  const pathname = usePathname();

  const items =
      role === "MANAGER"
          ? [
            ["홈", `/workplaces/${workplaceId}`],
            ["직원", `/workplaces/${workplaceId}/members`],
            ["근무표", `/workplaces/${workplaceId}/schedule`],
            ["승인", `/workplaces/${workplaceId}/substitutes/admin`],
          ]
          : [
            ["홈", `/workplaces/${workplaceId}`],
            ["내 근무", `/workplaces/${workplaceId}/my-shifts`],
            ["대체 요청", `/workplaces/${workplaceId}/substitutes/request`],
            ["받은 요청", `/workplaces/${workplaceId}/substitutes/received`],
          ];

  return (
      <nav className="fixed bottom-0 left-0 right-0 z-40 grid grid-cols-4 border-t border-[#dce8e2] bg-white lg:hidden">
        {items.map(([label, href]) => {
          const active = pathname === href;

          return (
              <Link
                  key={href}
                  href={href}
                  className={`py-3 text-center text-xs font-semibold ${
                      active
                          ? "text-[#005642]"
                          : "text-[#78847f]"
                  }`}
              >
                {label}
              </Link>
          );
        })}
      </nav>
  );
}