import type { AuthTokenResponse, CommunityComment, CommunityPostPage } from "./types";

const TOKEN_KEY = "lm_admin_access";
const REFRESH_KEY = "lm_admin_refresh";
const USER_KEY = "lm_admin_userId";

/** In dev, Vite proxies `/api` to the Java backend. In production, set `import.meta.env.VITE_API_BASE` at build time. */
function apiOrigin(): string {
  const base = import.meta.env.VITE_API_BASE as string | undefined;
  if (base && base.trim()) {
    return base.replace(/\/$/, "");
  }
  return "";
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
