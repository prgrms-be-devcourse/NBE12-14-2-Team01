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

// 서버가 토큰 만료(또는 틀린 토큰)일 때 주는 코드
const EXPIRED_TOKEN_CODE = "AUTH-006";

// 재발급하면 안 되는 경로 (무한 반복 방지)
const NO_REFRESH_PATHS = ["/auth/login", "/auth/signup", "/auth/refresh"];

// 진행 중인 재발급, 여러 요청이 동시에 만료돼도 한 번만 부르려고 같이 씀
let refreshing: Promise<string> | null = null;

// 요청 한 번 보내고 응답 해석함, 넘겨받은 토큰을 헤더에 붙임
async function request<T>(
    path: string,
    options: RequestInit,
    token: string | null
): Promise<T> {
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

// refreshToken 쿠키로 새 accessToken 받아서 저장, 이미 받는 중이면 그 결과를 같이 기다림
function refreshAccessToken(): Promise<string> {
  if (!refreshing) {
    refreshing = request<{ accessToken: string }>(
        "/auth/refresh",
        { method: "POST" },
        null
    )
        .then((data) => {
          setToken(data.accessToken);
          return data.accessToken;
        })
        .finally(() => {
          // 끝나면 비워서 다음 만료 때 다시 받을 수 있게
          refreshing = null;
        });
  }

  return refreshing;
}

// API 부를 때 이거 하나로 통일해서 씀. JSON 전용이라 body는 호출부에서 JSON.stringify() 해서 넘겨야 함
// 네트워크 끊기거나 요청 취소된 건 여기서 안 건드리고 그대로 흘려보냄
// 토큰 만료면 새로 받아서 한 번만 다시 보냄
export async function apiFetch<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  try {
    return await request<T>(path, options, token);
  } catch (error) {
    // 토큰 만료가 아니거나 재발급하면 안 되는 경로면 그대로 던짐 (토큰 없음 AUTH-004 등은 재발급 안 함)
    if (
        !(error instanceof ApiError) ||
        error.code !== EXPIRED_TOKEN_CODE ||
        NO_REFRESH_PATHS.includes(path)
    ) {
      throw error;
    }

    // 다른 요청이 이미 새 토큰을 받아뒀으면 재발급 없이 바로 다시 보냄
    if (getToken() === token) {
      try {
        await refreshAccessToken();
      } catch {
        // 재발급도 실패하면 처음 받은 만료 에러를 그대로 던짐
        throw error;
      }
    }

    return request<T>(path, options, getToken());
  }
}
