// 배포하면 이 주소 바꿔야 함
const BASE_URL = "http://localhost:8080/api/v1";

const TOKEN_KEY = "accessToken";

// 토큰은 쿠키로 안 오고 응답 body로 오니까 직접 저장해서 씀
export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// 백엔드 응답 형태 그대로
type ApiEnvelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

// 백엔드가 실패 응답 주면 이거 던짐
export class ApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

// API 부를 때 이거 하나로 통일해서 씀
export async function apiFetch<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  // headers를 객체 스프레드로 합치면 Headers나 배열로 넘어올 때 씹혀서 Headers로 정규화함
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(BASE_URL + path, {
    ...options,
    headers,
    credentials: "include",
  });

  const json: ApiEnvelope<T> = await response.json();

  if (!json.success) {
    throw new ApiError(json.code, json.message);
  }

  return json.data;
}
