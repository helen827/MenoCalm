import type { AuthTokenResponse, CommunityComment, CommunityPostPage } from "./types";

const TOKEN_KEY = "lm_admin_access";
const REFRESH_KEY = "lm_admin_refresh";
const USER_KEY = "lm_admin_userId";
const API_BASE_LS = "lm_admin_api_base";

/**
 * Call once on startup. If the URL contains `?api=https://host` (no trailing slash), it is saved to
 * localStorage and stripped from the address bar so the SPA can reach a remote API without a rebuild.
 */
export function bootstrapApiOriginFromLocation(): void {
  if (typeof window === "undefined") {
    return;
  }
  const params = new URLSearchParams(window.location.search);
  const raw = params.get("api")?.trim();
  if (!raw) {
    return;
  }
  if (!/^https?:\/\//i.test(raw)) {
    console.warn("lm_admin: ignored ?api= (must start with http:// or https://)");
    return;
  }
  const normalized = raw.replace(/\/$/, "");
  try {
    localStorage.setItem(API_BASE_LS, normalized);
  } catch {
    /* ignore quota / private mode */
  }
  try {
    const url = new URL(window.location.href);
    url.searchParams.delete("api");
    const qs = url.searchParams.toString();
    const next = `${url.pathname}${qs ? `?${qs}` : ""}${url.hash}`;
    window.history.replaceState({}, "", next || url.pathname);
  } catch {
    /* ignore */
  }
}

function readStoredApiBase(): string {
  try {
    const s = localStorage.getItem(API_BASE_LS);
    if (s?.trim()) {
      return s.trim().replace(/\/$/, "");
    }
  } catch {
    /* ignore */
  }
  return "";
}

/** In dev, Vite proxies `/api` to the Java backend. In production prefer `VITE_API_BASE` at build time, or `?api=` once. */
function apiOrigin(): string {
  const base = import.meta.env.VITE_API_BASE as string | undefined;
  if (base?.trim()) {
    return base.trim().replace(/\/$/, "");
  }
  const stored = readStoredApiBase();
  if (stored) {
    return stored;
  }
  return "";
}

export function getResolvedApiOrigin(): string {
  return apiOrigin();
}

export function getStoredAccessToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getStoredUserId(): string | null {
  return sessionStorage.getItem(USER_KEY);
}

export function persistSession(res: AuthTokenResponse): void {
  sessionStorage.setItem(TOKEN_KEY, res.accessToken);
  sessionStorage.setItem(REFRESH_KEY, res.refreshToken);
  const uid = res.userID ?? res.userId ?? "";
  sessionStorage.setItem(USER_KEY, uid);
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function setSessionFromToken(accessToken: string, userIdHint?: string): void {
  sessionStorage.setItem(TOKEN_KEY, accessToken);
  if (userIdHint) {
    sessionStorage.setItem(USER_KEY, userIdHint);
  }
}

async function refreshAccessToken(): Promise<boolean> {
  const refresh = sessionStorage.getItem(REFRESH_KEY);
  const userId = sessionStorage.getItem(USER_KEY);
  if (!refresh || !userId) {
    return false;
  }
  const url = `${apiOrigin()}/api/v1/auth/refresh`;
  const r = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: refresh, userId }),
  });
  if (!r.ok) {
    return false;
  }
  const res = (await r.json()) as AuthTokenResponse;
  persistSession(res);
  return true;
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const origin = apiOrigin();
  const url = path.startsWith("http") ? path : `${origin}${path}`;
  const headers = new Headers(init.headers);
  const token = getStoredAccessToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  let r = await fetch(url, { ...init, headers });
  if (r.status === 401) {
    const ok = await refreshAccessToken();
    if (ok) {
      const h2 = new Headers(init.headers);
      const t2 = getStoredAccessToken();
      if (t2) {
        h2.set("Authorization", `Bearer ${t2}`);
      }
      r = await fetch(url, { ...init, headers: h2 });
    }
  }
  return r;
}

export async function loginTestAccount(phone: string, secret: string): Promise<AuthTokenResponse> {
  const origin = apiOrigin();
  const url = `${origin}/api/v1/auth/test-account/login`;
  const r = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Test-Account-Secret": secret,
    },
    body: JSON.stringify({ phone }),
  });
  if (!r.ok) {
    const text = await r.text();
    throw new Error(text || `登录失败 HTTP ${r.status}`);
  }
  return (await r.json()) as AuthTokenResponse;
}

export async function fetchAdminPostsPage(cursor?: string | null, limit = 30): Promise<CommunityPostPage> {
  const q = new URLSearchParams();
  if (cursor) {
    q.set("cursor", cursor);
  }
  q.set("limit", String(limit));
  const path = `/api/v1/admin/community/posts/page?${q.toString()}`;
  const r = await apiFetch(path, { method: "GET" });
  if (!r.ok) {
    throw new Error(await r.text());
  }
  return (await r.json()) as CommunityPostPage;
}

export async function hideAdminPost(postId: string): Promise<void> {
  const r = await apiFetch(`/api/v1/admin/community/posts/${encodeURIComponent(postId)}`, {
    method: "DELETE",
  });
  if (!r.ok) {
    throw new Error(await r.text());
  }
}

export async function fetchPostComments(postId: string, limit = 80): Promise<CommunityComment[]> {
  const userId = getStoredUserId();
  if (!userId) {
    throw new Error("缺少 userId，请使用「测试账号登录」或粘贴 token 时填写用户 ID。");
  }
  const q = new URLSearchParams({ userId, limit: String(limit) });
  const r = await apiFetch(`/api/v1/community/posts/${encodeURIComponent(postId)}/comments?${q}`, {
    method: "GET",
  });
  if (!r.ok) {
    throw new Error(await r.text());
  }
  return (await r.json()) as CommunityComment[];
}

export async function hideAdminComment(postId: string, commentId: string): Promise<void> {
  const r = await apiFetch(
    `/api/v1/admin/community/posts/${encodeURIComponent(postId)}/comments/${encodeURIComponent(commentId)}`,
    { method: "DELETE" }
  );
  if (!r.ok) {
    throw new Error(await r.text());
  }
}
