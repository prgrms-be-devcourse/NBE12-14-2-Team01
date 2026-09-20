import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SWITCH",
  description: "소규모 업장을 위한 근무 및 대체 근무 관리 서비스",
};

export default function RootLayout({
                                     children,
                                   }: {
  children: React.ReactNode;
}) {
  return (
      <html lang="ko">
      <body>{children}</body>
      </html>
  );
}