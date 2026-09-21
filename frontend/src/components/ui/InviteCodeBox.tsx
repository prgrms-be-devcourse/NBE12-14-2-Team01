"use client";

import { useState } from "react";

type Props = {
  inviteCode?: string;
};

export default function InviteCodeBox({ inviteCode }: Props) {
  const [showCopyToast, setShowCopyToast] = useState(false);

  const handleCopyInviteCode = async () => {
    if (!inviteCode) {
      return;
    }

    await navigator.clipboard.writeText(inviteCode);

    setShowCopyToast(true);

    setTimeout(() => {
      setShowCopyToast(false);
    }, 2000);
  };

  return (
      <>
        <div className="rounded-xl bg-[#f3fbf7] p-4 text-center">
          <p className="text-xs text-[#78847f]">
            초대 코드
          </p>

          <p className="mt-1 text-lg font-black tracking-wider text-[#005642]">
            {inviteCode ?? "초대 코드를 불러오는 중입니다."}
          </p>
        </div>

        <button
            type="button"
            onClick={handleCopyInviteCode}
            disabled={!inviteCode}
            className="mt-4 w-full rounded-xl bg-[#005642] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#0b6b52] disabled:cursor-not-allowed disabled:bg-[#b8c4be]"
        >
          초대 코드 복사
        </button>

        {showCopyToast && (
            <div className="fixed bottom-6 right-6 z-50 rounded-xl bg-[#005642] px-5 py-3 text-sm font-bold text-white shadow-lg">
              초대 코드가 복사되었습니다!
            </div>
        )}
      </>
  );
}