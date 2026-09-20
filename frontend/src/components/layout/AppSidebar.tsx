"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  getNavItems,
  WorkplaceRole,
} from "@/lib/navigation";

type Props = {
  workplaceId: string;
  role: WorkplaceRole;
};

export default function AppSidebar({
                                     workplaceId,
                                     role,
                                   }: Props) {
  const pathname = usePathname();

  const items = getNavItems(workplaceId, role);
  const dashboardHref = `/workplaces/${workplaceId}`;

  return (
      <aside className="fixed left-0 top-0 hidden h-screen w-64 border-r border-[#dce8e2] bg-white lg:flex lg:flex-col">
        <div className="flex h-20 items-center px-6">
          <Link
              href={dashboardHref}
              className="text-2xl font-black tracking-tight text-[#005642]"
          >
            SWITCH
          </Link>
        </div>

        <nav className="flex-1 space-y-1 px-3">
          {items.map((item) => {
            const active =
                item.href === dashboardHref
                    ? pathname === item.href
                    : pathname.startsWith(item.href);

            return (
                <Link
                    key={item.href}
                    href={item.href}
                    className={`block rounded-xl px-4 py-3 text-sm font-semibold transition ${
                        active
                            ? "bg-[#dff7ec] text-[#005642]"
                            : "text-[#66736d] hover:bg-[#f3fbf7] hover:text-[#005642]"
                    }`}
                >
                  {item.label}
                </Link>
            );
          })}
        </nav>

        <div className="m-4 rounded-2xl bg-[#f3fbf7] p-4 text-sm leading-6 text-[#607069]">
          <strong className="text-[#005642]">SWITCH</strong>
          <br />
          근무가 필요한 순간,
          <br />
          더 유연한 하루 :)
        </div>

      </aside>
  );
}