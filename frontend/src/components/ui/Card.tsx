type Props = {
  children: React.ReactNode;
  className?: string;
};

export default function Card({ children, className = "" }: Props) {
  return (
      <section
          className={`rounded-2xl border border-[#dce8e2] bg-white p-5 shadow-[0_12px_32px_rgba(0,74,54,0.06)] ${className}`}
      >
        {children}
      </section>
  );
}