"use client";

import Image from "next/image";
import Link from "next/link";
import {
  useEffect,
  useState,
} from "react";
import { usePathname } from "next/navigation";
import switchIcon from "@/app/icon.png";
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

  const [isOpen, setIsOpen] =
      useState(false);

  const items = getNavItems(
      workplaceId,
      role
  );

  const dashboardHref =
      `/workplaces/${workplaceId}`;

  /*
   * 페이지가 이동되면
   * 모바일 사이드바를 자동으로 닫는다.
   */
  useEffect(() => {
    setIsOpen(false);
  }, [pathname]);

  /*
   * 모바일 사이드바가 열렸을 때
   * 뒤쪽 화면이 스크롤되지 않도록 한다.
   */
  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const originalOverflow =
        document.body.style.overflow;

    document.body.style.overflow =
        "hidden";

    return () => {
      document.body.style.overflow =
          originalOverflow;
    };
  }, [isOpen]);

  return (
      <>
        {/* 모바일 햄버거 버튼 */}
        {!isOpen && (
            <button
                type="button"
                onClick={() =>
                    setIsOpen(true)
                }
                className="fixed left-4 top-4 z-[70] grid h-10 w-10 place-items-center rounded-xl border border-[#dce8e2] bg-white text-xl font-bold text-[#005642] shadow-sm transition hover:bg-[#f3fbf7] lg:hidden"
                aria-label="메뉴 열기"
                aria-expanded={isOpen}
            >
              ☰
            </button>
        )}

        {/* 모바일 배경 */}
        <div
            onClick={() =>
                setIsOpen(false)
            }
            className={`fixed inset-0 z-50 bg-black/30 transition-opacity duration-200 lg:hidden ${
                isOpen
                    ? "pointer-events-auto opacity-100"
                    : "pointer-events-none opacity-0"
            }`}
        />

        {/* 모바일 사이드바 */}
        <aside
            className={`fixed left-0 top-0 z-[60] flex h-screen w-[280px] max-w-[85vw] flex-col border-r border-[#dce8e2] bg-white shadow-xl transition-transform duration-200 lg:hidden ${
                isOpen
                    ? "translate-x-0"
                    : "-translate-x-full"
            }`}
        >
          {/* 상단 */}
          <div className="flex h-20 items-center justify-between px-6">
            <Link
                href={dashboardHref}
                onClick={() =>
                    setIsOpen(false)
                }
                className="flex items-center gap-2 text-2xl font-black tracking-tight text-[#005642]"
            >
              <Image
                  src={switchIcon}
                  alt=""
                  width={32}
                  height={32}
                  className="h-8 w-8 object-contain"
              />

              SWITCH
            </Link>

            <button
                type="button"
                onClick={() =>
                    setIsOpen(false)
                }
                className="grid h-9 w-9 place-items-center rounded-lg text-2xl text-[#78847f] transition hover:bg-[#f3fbf7] hover:text-[#005642]"
                aria-label="메뉴 닫기"
            >
              ×
            </button>
          </div>

          {/* 메뉴 */}
          <nav className="flex-1 space-y-1 overflow-y-auto px-3 pb-4">
            {items.map((item) => {
              const active =
                  item.href ===
                  dashboardHref
                      ? pathname === item.href
                      : pathname.startsWith(
                          item.href
                      );

              return (
                  <Link
                      key={item.href}
                      href={item.href}
                      onClick={() =>
                          setIsOpen(false)
                      }
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

          {/* 하단 */}
          <div className="m-4 rounded-2xl bg-[#f3fbf7] p-4 text-sm leading-6 text-[#607069]">
            <strong className="text-[#005642]">
              SWITCH
            </strong>

            <br />

            근무가 필요한 순간,

            <br />

            더 유연한 하루 :)
          </div>
        </aside>

        {/* 데스크톱 사이드바 */}
        <aside className="fixed left-0 top-0 z-30 hidden h-screen w-64 border-r border-[#dce8e2] bg-white lg:flex lg:flex-col">
          <div className="flex h-20 items-center px-6">
            <Link
                href={dashboardHref}
                className="flex items-center gap-2 text-2xl font-black tracking-tight text-[#005642]"
            >
              <Image
                  src={switchIcon}
                  alt=""
                  width={32}
                  height={32}
                  className="h-8 w-8 object-contain"
              />

              SWITCH
            </Link>
          </div>

          <nav className="flex-1 space-y-1 px-3">
            {items.map((item) => {
              const active =
                  item.href ===
                  dashboardHref
                      ? pathname === item.href
                      : pathname.startsWith(
                          item.href
                      );

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
            <strong className="text-[#005642]">
              SWITCH
            </strong>

            <br />

            근무가 필요한 순간,

            <br />

            더 유연한 하루 :)
          </div>
        </aside>
      </>
  );
}