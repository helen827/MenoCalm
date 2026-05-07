import { useCallback, useState } from "react";
import { clearSession, getStoredAccessToken, persistSession, setSessionFromToken } from "./api";
import type { AuthTokenResponse } from "./types";
import { LoginPanel } from "./LoginPanel";
import { PostsModeration } from "./PostsModeration";

export default function App() {
  const [token, setToken] = useState<string | null>(() => getStoredAccessToken());

  const handleTestLogin = useCallback((res: AuthTokenResponse) => {
    persistSession(res);
    setToken(res.accessToken);
  }, []);

  const handleTokenLogin = useCallback((accessToken: string, userId: string) => {
    setSessionFromToken(accessToken, userId);
    setToken(accessToken);
  }, []);

  const handleLogout = useCallback(() => {
    clearSession();
    setToken(null);
  }, []);

  return (
    <div className="app-shell">
      <header style={{ marginBottom: 20 }}>
        <h1 style={{ margin: "0 0 8px", fontSize: 22 }}>潮安 · 社区运营后台</h1>
        <p style={{ margin: 0, color: "#555", fontSize: 14 }}>
          管理帖子与评论（需在后端配置 <code>COMMUNITY_ADMIN_USER_IDS</code> 包含当前登录用户 ID）。
        </p>
      </header>

      {!token ? (
        <LoginPanel onTestLogin={handleTestLogin} onTokenLogin={handleTokenLogin} />
      ) : (
        <PostsModeration onLogout={handleLogout} />
      )}
    </div>
  );
}
