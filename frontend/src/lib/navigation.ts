export type WorkplaceRole = "MANAGER" | "EMPLOYEE";

export type NavItem = {
  label: string;
  href: (workplaceId: string) => string;
  managerOnly?: boolean;
};

export const navItems: NavItem[] = [
  {
    label: "대시보드",
    href: (id) => `/workplaces/${id}`,
  },
  {
    label: "직원 목록",
    href: (id) => `/workplaces/${id}/members`,
    managerOnly: true,
  },
  {
    label: "정기 근무",
    href: (id) => `/workplaces/${id}/regular-shifts`,
    managerOnly: true,
  },
  {
    label: "근무표",
    href: (id) => `/workplaces/${id}/schedule`,
    managerOnly: true,
  },
  {
    label: "내 근무",
    href: (id) => `/workplaces/${id}/my-shifts`,
  },
  {
    label: "불가능 일정",
    href: (id) => `/workplaces/${id}/unavailable`,
  },
  {
    label: "대체 근무",
    href: (id) => `/workplaces/${id}/substitutes/request`,
  },
  {
    label: "받은 요청",
    href: (id) => `/workplaces/${id}/substitutes/received`,
  },
  {
    label: "승인 관리",
    href: (id) => `/workplaces/${id}/substitutes/admin`,
    managerOnly: true,
  },
];

export function getNavItems(workplaceId: string, role: WorkplaceRole) {
  return navItems
  .filter((item) => !item.managerOnly || role === "MANAGER")
  .map((item) => ({
    label: item.label,
    href: item.href(workplaceId),
  }));
}