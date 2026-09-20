import { WorkplaceRole } from "@/lib/navigation";

type Props = {
  workplaceName: string;
  userName: string;
  role: WorkplaceRole;
};

export default function AppHeader({
                                    workplaceName,
                                    userName,
                                    role,
                                  }: Props) {
  return (
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-[#dce8e2] bg-white/90 px-4 backdrop-blur lg:px-8">
        <div>
          <p className="text-xs text-[#78847f]">현재 근무지</p>
          <p className="font-bold text-[#202a27]">
            {workplaceName}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
              type="button"
              className="grid h-9 w-9 place-items-center rounded-full hover:bg-[#f3fbf7]"
          >
            🔔
          </button>

          <div className="grid h-9 w-9 place-items-center rounded-full bg-[#dff7ec] font-bold text-[#005642]">
            {userName.slice(0, 1)}
          </div>

          <div className="hidden sm:block">
            <p className="text-sm font-bold">{userName}</p>
            <p className="text-xs text-[#78847f]">
              {role === "MANAGER" ? "관리자" : "직원"}
            </p>
          </div>
        </div>
      </header>
  );
}