# Step 1 审计：UserDefaults 盘点与迁移方案

> 对应计划：`docs/Audit-First-Execution-Plan.md` 的 Step 1  
> 目标：明确哪些数据继续放在 `UserDefaults`，哪些必须迁移到本地数据库（SwiftData/Core Data/SQLite），并给出可执行迁移策略。  
> 约束：本阶段只做审计和方案，不改业务代码。

---

## 1. 结论摘要（先看）

- 当前项目中，`UserDefaults` 同时承担了**配置数据**和**业务核心数据**两类职责。
- 配置类数据可继续保留在 `UserDefaults`。
- 业务核心数据（记录、洞察、报告、同步轨迹、练习记录）应迁移到本地数据库。
- 鉴权 token 不应继续放在 `UserDefaults`，应迁移到 `Keychain`（安全项）。

---

## 2. 当前 UserDefaults 使用点全量盘点

### 2.1 业务核心数据（应迁移到本地数据库）

| 逻辑数据 | Key（逻辑名） | 当前实现位置 | 数据类型/内容 | 建议 |
|---|---|---|---|---|
| 日记列表 | `chaoan_journal_entries_v1`（含 scoped） | `Data/Repositories/UserDefaultsJournalRepository.swift` | `[JournalEntry]` | 迁移到 DB |
| 最近一条日记 | `chaoan_journal_latest_v1`（含 scoped） | `Data/Repositories/UserDefaultsJournalRepository.swift` | `JournalEntry` | 迁移到 DB（可由查询替代） |
| 对话结构化洞察 | `chaoan_conversation_insights_v1`（scoped） | `Data/Repositories/UserDefaultsConversationInsightRepository.swift` | `[ConversationInsight]` | 迁移到 DB |
| 报告快照 | `chaoan_report_snapshots_v1`（scoped） | `Data/Repositories/UserDefaultsReportSnapshotRepository.swift` | `[ReportSnapshot]` | 迁移到 DB |
| 同步待处理队列 | `chaoan_sync_queue_pending_v1`（scoped） | `Data/Services/SyncQueue.swift` | `[SyncUploadTask]` | 迁移到 DB |
| 同步失败任务 | `chaoan_sync_queue_failed_v1`（scoped） | `Data/Services/SyncQueue.swift` | `[String]` | 迁移到 DB |
| 同步历史轨迹 | `chaoan_sync_queue_history_v1`（scoped） | `Data/Services/SyncQueue.swift` | `[SyncTaskTrace]` | 迁移到 DB |
| 呼吸练习累计次数 | `chaoan_practice_total_sessions_v1`（scoped） | `App/AppState.swift` | `Int` | 迁移到 DB |
| 呼吸练习最近完成时间 | `chaoan_practice_last_completed_at_v1`（scoped） | `App/AppState.swift` | `TimeInterval` | 迁移到 DB |

备注：
- `Journal` 存在 legacy -> scoped 迁移逻辑，说明已有历史数据版本包袱。
- 这些数据都属于可增长、可查询、可同步的业务域，不适合长期放在 K/V。

### 2.2 配置类数据（适合继续放在 UserDefaults）

| 配置项 | Key | 当前实现位置 | 用途 | 建议 |
|---|---|---|---|---|
| 运行环境 | `chaoan_runtime_env` | `App/AppRuntimeEnvironment.swift` | dev/staging/prod 选择 | 保留 |
| 后端地址（legacy） | `chaoan_backend_base_url` | `App/AppRuntimeEnvironment.swift` | 兼容旧配置 | 保留（逐步废弃） |
| 后端地址（分环境） | `chaoan_backend_base_url_{env}` | `App/AppRuntimeEnvironment.swift` | 分环境后端地址 | 保留 |
| 请求超时 | `chaoan_backend_timeout_seconds` | `App/AppRuntimeEnvironment.swift` | 网络超时配置 | 保留 |

### 2.3 安全敏感数据（不应放 UserDefaults）

| 数据 | Key | 当前实现位置 | 建议 |
|---|---|---|---|
| access/refresh token | `chaoan_auth_tokens_v1`（scoped） | `Data/Services/AuthTokenManager.swift` | 迁移到 `Keychain` |

---

## 3. 保留 vs 迁移分类结果

## 3.1 继续保留在 UserDefaults

- 环境和网络配置类（`runtime_env`、`backend_url`、`timeout`）。
- 轻量开关类（后续如 feature flags 本地 override 也可保留）。

## 3.2 迁移到本地数据库

- 日记、洞察、报告、同步队列、练习记录等所有业务实体数据。
- 原则：凡是需要**查询、聚合、历史追踪、同步冲突处理**的数据，一律数据库化。

## 3.3 迁移到 Keychain

- 鉴权 token（access/refresh、过期时间元数据）。

---

## 4. 迁移方案（可执行）

## 4.1 目标状态

- `UserDefaults`：仅保存配置类数据。
- 本地数据库：保存所有业务实体。
- `Keychain`：保存 token 与会话敏感数据。

## 4.2 迁移步骤

1. **建迁移映射表**
   - 为每个旧 key 映射到新模型和字段。
2. **实现一次性首启迁移（idempotent）**
   - 读取旧 `UserDefaults` 数据，写入 DB/Keychain。
   - 写入 `migrationVersion` 标记，防止重复迁移。
3. **过渡期双读校验（短窗口）**
   - 以 DB 为主读，必要时只读 fallback 旧数据并记录告警。
4. **切换为单读单写**
   - 业务层只通过 DB/Keychain。
5. **清理旧业务 key**
   - 仅清理业务数据 key，保留配置 key。

## 4.3 数据一致性校验点

- 日记条数、洞察条数、报告快照条数迁移前后相等。
- 同步 pending/failed/history 迁移后统计一致。
- 呼吸练习累计值和最后完成时间迁移后一致。
- token 可读且刷新链路正常（迁移到 Keychain 后）。

## 4.4 回滚策略

- 迁移版本号控制：失败时不提升版本号。
- 保留只读 fallback 窗口（一个小版本）。
- 关键异常时可切回旧读取路径（只读），禁止继续写旧存储以免分叉。

---

## 5. 风险清单与控制

| 风险 | 描述 | 控制措施 |
|---|---|---|
| 数据丢失 | 迁移脚本异常导致部分记录未落库 | 迁移前后条数/hash 对账 |
| 多用户串号 | scoped key 导入 userID 错配 | 迁移按 userID 分批、逐用户校验 |
| 队列状态错乱 | 迁移时同步任务仍在 flush | 迁移窗口冻结队列处理 |
| 安全风险 | token 仍在 UserDefaults | 独立优先迁移到 Keychain |

---

## 6. 本步骤验收结果

- [x] 已完成 UserDefaults 使用点全量清单
- [x] 已完成保留 vs 迁移分类
- [x] 已完成迁移步骤、校验点、回滚策略定义
- [x] 明确安全项：token 迁移 Keychain

下一步：执行 Step 2，产出 SwiftData 数据模型设计（字段/关系/同步字段）。

