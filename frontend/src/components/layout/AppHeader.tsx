"use client";

import { useState } from "react";
import {
  useParams,
  useRouter,
} from "next/navigation";

type Notification = {
  id: number;
  message: string;
  time: string;
  read: boolean;
  path: string;
};

const initialNotifications: Notification[] = [
  {
    id: 1,
    message:
        "이서연님이 대체 근무 요청을 수락했습니다.",
    time: "방금 전",
    read: false,
    path: "substitutes/admin",
  },
  {
    id: 2,
    message:
        "대체 근무 후보의 응답을 확인해주세요.",
    time: "10분 전",
    read: false,
    path: "substitutes/admin",
  },
  {
    id: 3,
    message:
        "이번 주 근무표가 공개되었습니다.",
    time: "1시간 전",
    read: true,
    path: "schedule",
  },
];

export default function AppHeader() {
  const router = useRouter();
  const params = useParams();

  const workplaceId =
      params.workplaceId as string;

  const [
    showProfileMenu,
    setShowProfileMenu,
  ] = useState(false);

  const [
    showNotificationMenu,
    setShowNotificationMenu,
  ] = useState(false);

  const [
    notifications,
    setNotifications,
  ] = useState<Notification[]>(
      initialNotifications
  );

  const unreadCount =
      notifications.filter(
          (notification) =>
              !notification.read
      ).length;

  const handleLogout = () => {
    /*
     * TODO: 인증 API 연동 후
     * 실제 로그아웃 처리 추가
     */
    router.push("/login");
  };

  const handleNotificationClick = (
      notification: Notification
  ) => {
    setNotifications((prev) =>
        prev.map((item) =>
            item.id === notification.id
                ? {
                  ...item,
                  read: true,
                }
                : item
        )
    );

    setShowNotificationMenu(false);

    router.push(
        `/workplaces/${workplaceId}/${notification.path}`
    );
  };

  const handleReadAll = () => {
    setNotifications((prev) =>
        prev.map((notification) => ({
          ...notification,
          read: true,
        }))
    );
  };

  return (
      <header className="relative z-40 flex h-[76px] items-center justify-between border-b border-[#dce8e2] bg-white pl-20 pr-4 sm:pr-6 lg:px-8">
        {/* 현재 근무지 */}
        <div>
          <p className="text-xs font-semibold text-[#78847f]">
            현재 근무지
          </p>

          <p className="mt-0.5 font-black">
            카페 스위치
          </p>
        </div>

        {/* 우측 */}
        <div className="flex items-center gap-3 sm:gap-5">
          {/* 알림 */}
          <div className="relative">
            <button
                type="button"
                onClick={() => {
                  setShowNotificationMenu(
                      (prev) => !prev
                  );

                  setShowProfileMenu(false);
                }}
                className="relative grid h-10 w-10 place-items-center rounded-xl text-lg transition hover:bg-[#f3fbf7]"
                aria-label="알림"
            >
              🔔

              {unreadCount > 0 && (
                  <span className="absolute right-0.5 top-0.5 grid min-h-[16px] min-w-[16px] place-items-center rounded-full bg-[#d95555] px-1 text-[10px] font-bold leading-none text-white">
                {unreadCount}
              </span>
              )}
            </button>

            {/* 알림 메뉴 */}
            {showNotificationMenu && (
                <div className="absolute right-0 top-[calc(100%+10px)] z-50 w-[340px] max-w-[calc(100vw-24px)] overflow-hidden rounded-2xl border border-[#dce8e2] bg-white shadow-xl">
                  <div className="flex items-center justify-between border-b border-[#edf2ef] px-4 py-4">
                    <div className="flex items-center gap-2">
                      <p className="font-black">
                        알림
                      </p>

                      {unreadCount > 0 && (
                          <span className="rounded-full bg-[#dff7ec] px-2 py-0.5 text-xs font-bold text-[#14956c]">
                      {unreadCount}
                    </span>
                      )}
                    </div>

                    {unreadCount > 0 && (
                        <button
                            type="button"
                            onClick={handleReadAll}
                            className="text-xs font-bold text-[#14956c] hover:underline"
                        >
                          모두 읽음
                        </button>
                    )}
                  </div>

                  <div className="max-h-[360px] overflow-y-auto">
                    {notifications.length >
                    0 ? (
                        notifications.map(
                            (notification) => (
                                <button
                                    key={
                                      notification.id
                                    }
                                    type="button"
                                    onClick={() =>
                                        handleNotificationClick(
                                            notification
                                        )
                                    }
                                    className={`flex w-full gap-3 border-b border-[#edf2ef] px-4 py-4 text-left transition last:border-b-0 hover:bg-[#f3fbf7] ${
                                        notification.read
                                            ? "bg-white"
                                            : "bg-[#f7fcf9]"
                                    }`}
                                >
                                  <div className="pt-2">
                          <span
                              className={`block h-2 w-2 rounded-full ${
                                  notification.read
                                      ? "bg-transparent"
                                      : "bg-[#14956c]"
                              }`}
                          />
                                  </div>

                                  <div className="min-w-0 flex-1">
                                    <p
                                        className={`text-sm leading-6 ${
                                            notification.read
                                                ? "font-medium text-[#66736d]"
                                                : "font-bold text-[#1f2925]"
                                        }`}
                                    >
                                      {
                                        notification.message
                                      }
                                    </p>

                                    <p className="mt-1 text-xs text-[#9aa5a0]">
                                      {
                                        notification.time
                                      }
                                    </p>
                                  </div>
                                </button>
                            )
                        )
                    ) : (
                        <div className="px-4 py-10 text-center">
                          <p className="text-sm font-bold text-[#66736d]">
                            새로운 알림이 없어요.
                          </p>
                        </div>
                    )}
                  </div>

                  {/*
               * TODO:
               * API 연동 후 mock 알림 데이터를
               * 실제 알림 조회 결과로 변경한다.
               *
               * 읽음 처리 역시 서버 API와 연결한다.
               */}
                </div>
            )}
          </div>

          {/* 프로필 */}
          <div className="relative">
            <button
                type="button"
                onClick={() => {
                  setShowProfileMenu(
                      (prev) => !prev
                  );

                  setShowNotificationMenu(
                      false
                  );
                }}
                className="flex items-center gap-3 rounded-xl px-2 py-1.5 text-left transition hover:bg-[#f3fbf7]"
            >
              <div className="grid h-10 w-10 place-items-center rounded-full bg-[#dff7ec] font-black text-[#005642]">
                김
              </div>

              <div className="hidden sm:block">
                <p className="text-sm font-black">
                  김지연
                </p>

                <p className="text-xs text-[#78847f]">
                  관리자
                </p>
              </div>

              <span
                  className={`hidden text-xs text-[#78847f] transition-transform sm:block ${
                      showProfileMenu
                          ? "rotate-180"
                          : ""
                  }`}
              >
              ▾
            </span>
            </button>

            {/* 프로필 메뉴 */}
            {showProfileMenu && (
                <div className="absolute right-0 top-[calc(100%+8px)] z-50 w-52 overflow-hidden rounded-xl border border-[#dce8e2] bg-white shadow-lg">
                  <div className="border-b border-[#edf2ef] px-4 py-3">
                    <p className="text-sm font-black">
                      김지연
                    </p>

                    <p className="mt-0.5 text-xs text-[#78847f]">
                      관리자 · 카페 스위치
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={handleLogout}
                      className="w-full px-4 py-3 text-left text-sm font-bold text-[#d95555] transition hover:bg-[#fff5f5]"
                  >
                    로그아웃
                  </button>
                </div>
            )}
          </div>
        </div>
      </header>
  );
}