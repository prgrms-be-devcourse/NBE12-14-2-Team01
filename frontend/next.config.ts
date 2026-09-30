import type { NextConfig } from "next";

// 배포 때 BACKEND_URL을 넣으면 /api 요청을 백엔드로 넘김 (없으면 로컬처럼 백엔드 주소를 직접 부름)
const backendUrl = process.env.BACKEND_URL;

const nextConfig: NextConfig = {
  reactCompiler: true,
  async rewrites() {
    if (!backendUrl) {
      return [];
    }

    return [
      {
        source: "/api/v1/:path*",
        destination: `${backendUrl}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
