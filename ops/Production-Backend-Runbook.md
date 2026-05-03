# 生产与 Staging 后端联调 Runbook

## 目标

- Staging / Production **不得依赖** `InMemoryAuthAPIClient`：必须在设备或 `UserDefaults` 中配置有效 `HTTPS` 后端（见 [潮安/App/AppRuntimeEnvironment.swift](../潮安/App/AppRuntimeEnvironment.swift)）。
- Journal 与社区 Feed 与同一 `baseURL` 约定一致（路径见下）。

## 配置键（UserDefaults）

| Key | 说明 |
|-----|------|
| `chaoan_runtime_env` | `dev` / `staging` / `prod` |
| `chaoan_backend_base_url_staging` 等 | 分环境 URL，优先于 legacy `chaoan_backend_base_url` |
| `chaoan_backend_timeout_seconds` | 可选，默认 8 |

Production 下 `resolve()` 会拒绝缺失 URL、非 HTTPS、`localhost`；调试时若 `resolve()` 抛错，应用会走 `resolveOrFallback()` 退回 dev（**仅用于开发**），发布前须在真机验证 `resolve()` 成功。

## API 约定（客户端已实现）

### 鉴权

- 由 `HTTPAuthAPIClient` / `AuthTokenManager` 负责登录与刷新；401 后刷新并重试 Journal 请求。

### Journal

- 与现有 `HTTPRemoteJournalAPIClient` 一致（项目内路径以工程为准）。

### 社区 Feed（可选）

- `GET {baseURL}/community/feed`
- 响应 JSON 与包内 [community_feed.json](../潮安/Resources/community_feed.json) 同结构：`{ "official": [...], "user": [...] }`
- 失败时客户端自动回退包内 JSON。

## 观测与告警

- 在 Profile / 内部面板查看 `OperationalAlertingCenter` 导出的事件与告警。
- 线上需独立接入崩溃与网络指标（PRD §7.2），并与本 Runbook 的「联调完成」项一并勾选 [Release-Acceptance-Checklist](./Release-Acceptance-Checklist.md)。

## 联调检查清单

- [ ] Staging：`chaoan_runtime_env=staging`，HTTPS base URL 可登录、可写 Journal、Feed 200 或回退 bundled
- [ ] Prod：`resolve()` 无 fallback，无 InMemory 鉴权
- [ ] 401 / 刷新失败：会话清理与用户提示符合预期
- [ ] 429 / 5xx：错误映射与日志字段可定位
