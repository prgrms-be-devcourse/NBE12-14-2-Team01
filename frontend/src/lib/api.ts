// NEXT_PUBLIC_API_URL에 /api/v1까지 포함해서 설정하면 됨, 안 하면 로컬 주소 씀
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

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

// 백엔드가 실패 응답 주면 이거 던짐, data도 같이 담아둬서 필요하면 꺼내 쓸 수 있음
export class ApiError extends Error {
  code: string;
  status: number;
  data: unknown;

  constructor(code: string, message: string, status: number, data: unknown) {
    super(message);
    this.code = code;
    this.status = status;
    this.data = data;
  }
}

// 응답이 우리가 아는 형태가 아닐 때 던지는 에러 (서버 다운, 프록시 에러 페이지 등)
export class UnexpectedResponseError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// API 부를 때 이거 하나로 통일해서 씀. JSON 전용이라 body는 호출부에서 JSON.stringify() 해서 넘겨야 함
// 네트워크 끊기거나 요청 취소된 건 여기서 안 건드리고 그대로 흘려보냄
export async function apiFetch<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  // headers를 객체 스프레드로 합치면 Headers나 배열로 넘어올 때 씹혀서 Headers로 정규화함
  const headers = new Headers(options.headers);
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(BASE_URL + path, {
    ...options,
    headers,
    credentials: "include",
  });

  let json: ApiEnvelope<T>;
  try {
    json = await response.json();
  } catch (error) {
    if (!(error instanceof SyntaxError)) {
      throw error;
    }

    throw new UnexpectedResponseError(
        response.status,
        "서버 응답을 해석할 수 없습니다."
    );
  }

  if (typeof json?.success !== "boolean") {
    throw new UnexpectedResponseError(
        response.status,
        "예상하지 못한 응답 형식입니다."
    );
  }

  if (!json.success) {
    throw new ApiError(json.code, json.message, response.status, json.data);
  }

  return json.data;
}
