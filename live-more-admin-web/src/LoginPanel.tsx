import { useState } from "react";
import { getResolvedApiOrigin, loginTestAccount } from "./api";
import type { AuthTokenResponse } from "./types";

type Props = {
  onTestLogin: (res: AuthTokenResponse) => void;
  onTokenLogin: (accessToken: string, userId: string) => void;
};

export function LoginPanel({ onTestLogin, onTokenLogin }: Props) {
  const [mode, setMode] = useState<"test" | "token">("test");
  const [phone, setPhone] = useState("");
  const [secret, setSecret] = useState("");
  const [token, setToken] = useState("");
  const [userId, setUserId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submitTest(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await loginTestAccount(phone.trim(), secret);
      onTestLogin(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  function submitToken(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const t = token.trim();
    const u = userId.trim();
    if (!t) {
      setError("请粘贴 access token。");
      return;
    }
    if (!u) {
      setError("请填写用户 ID（与 JWT subject 一致，例如 phone_13800138000），用于加载评论列表。");
      return;
    }
    onTokenLogin(t, u);
  }

  const apiBase = getResolvedApiOrigin();

  return (
    <div className="card">
      {!apiBase ? (
        <p
          style={{
            margin: "0 0 16px",
            padding: 12,
            background: "#fff8e6",
            border: "1px solid #f0d090",
            borderRadius: 8,
            fontSize: 13,
            color: "#5c4a00",
          }}
        >
          尚未配置 API 根地址。任选其一：在 Render 该静态站的 Environment 中设置{" "}
          <code>VITE_API_BASE</code> 后重新部署；或首次用带参数的地址打开本站（会自动保存并去掉参数）：{" "}
          <code>?api=https://你的-live-more-api-根地址</code>
          （不要末尾 <code>/</code>）。配置好后端时，请把本站来源加入{" "}
          <code>CORS_ALLOWED_ORIGINS</code>。
        </p>
      ) : null}
      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        <button
          type="button"
          className={mode === "test" ? "btn-primary" : "btn-ghost"}
          onClick={() => setMode("test")}
        >
          测试账号登录
        </button>
        <button
          type="button"
          className={mode === "token" ? "btn-primary" : "btn-ghost"}
          onClick={() => setMode("token")}
        >
          粘贴 Token
        </button>
      </div>

      {mode === "test" ? (
        <form onSubmit={submitTest}>
          <label htmlFor="phone">手机号</label>
          <input
            id="phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="13800138000"
            autoComplete="username"
          />
          <div style={{ height: 12 }} />
          <label htmlFor="secret">测试账号密钥（对应后端 X-Test-Account-Secret / TEST_ACCOUNT_SECRET）</label>
          <input
            id="secret"
            type="password"
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
            autoComplete="current-password"
          />
          <p className="hint">需在后端开启 <code>TEST_ACCOUNT_LOGIN_ENABLED</code> 并配置密钥。</p>
          {error ? <p className="err">{error}</p> : null}
          <div style={{ marginTop: 16 }}>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? "登录中…" : "登录"}
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={submitToken}>
          <label htmlFor="access">Access Token（Bearer）</label>
          <textarea id="access" value={token} onChange={(e) => setToken(e.target.value)} placeholder="eyJ…" />
          <div style={{ height: 12 }} />
          <label htmlFor="uid">用户 ID（JWT sub）</label>
          <input id="uid" value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="phone_13800138000" />
          <p className="hint">从 App 调试或登录响应中的 userID 复制；必须与该 token 对应。</p>
          {error ? <p className="err">{error}</p> : null}
          <div style={{ marginTop: 16 }}>
            <button type="submit" className="btn-primary">
              进入后台
            </button>
          </div>
        </form>
      )}

      <p className="hint" style={{ marginTop: 20 }}>
        本地开发：先启动 <code>live-more-api</code>，再在本目录执行 <code>npm run dev</code>。请求会通过 Vite 代理到{" "}
        <code>http://127.0.0.1:8080</code>（可用环境变量 <code>VITE_DEV_API_PROXY</code> 修改）。
      </p>
    </div>
  );
}
