import type { ReactNode } from "react";

type Props = {
  title: string;
  description?: string;
  children?: ReactNode;
};

export default function PageHeader({
    title,
    description,
    children,
    }: Props) {
  return (
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-[#202a27]">
            {title}
          </h1>

          {description && (
              <p className="mt-1 text-sm text-[#78847f]">
                {description}
              </p>
          )}
        </div>

        {children}
      </div>
  );
}