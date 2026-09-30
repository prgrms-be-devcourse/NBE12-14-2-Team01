import { redirect } from "next/navigation";

// 첫 화면은 근무지 목록으로 (로그인 안 했으면 거기서 로그인 화면으로 보냄)
export default function Home() {
  redirect("/workplaces");
}
