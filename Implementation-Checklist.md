# 潮安 Refactor Implementation Checklist

## Phase 1: 核心边界建立（已完成）

- 引入 `AppRouter`，解耦导航状态与业务状态
- 提取 `JournalRepositoryProtocol`
- 提供 `UserDefaultsJournalRepository` 默认实现
- 提取 `ExtractSignalsUseCaseProtocol`
- 提供 `RuleBasedExtractSignalsUseCase` 实现
- `AppViewModel` 通过依赖注入连接 Repository + UseCase
- 增加关键单测（Router、UseCase、ViewModel）
- 增加基础 UI smoke test（onboarding -> login -> home）

## Phase 2: 文件级模块化（已完成）

- 移除历史单体文件（`AppPrototypeViews.swift`）
- 按 Feature 拆分页面文件（ChatJournal / Reports / Community / Practice / Profile / Onboarding / Historical）
- 抽离 `Theme` 与 `SharedUI`
- 抽离 `Models`、`UseCases`、`Repository`
- 目录归位到 `App/Core/Domain/Data/Features`
- 编译验证通过（目录迁移后）

## 当前状态检查（已完成）

- 新目录结构与 Architecture Guide 对齐
- 无新增 lint 报错
- 功能行为保持不变（结构迁移不改业务）

## Phase 3: 可选增强（待开始）

- 补充更多 Domain 单测（报告汇总、边界输入）
- Features 内继续组件化，控制单文件体积
- 增加 CI（build + tests）自动门禁
- 评估 Swift Package 模块化的切入点

## Phase 3 跟踪看板（2026-04-24）

### 当前体量基线（按行数）

- `Features/Reports/ReportsViews.swift`: 187
- `Features/Profile/ProfileViews.swift`: 153
- `Features/Practice/PracticeView.swift`: 130
- `Features/Onboarding/IntroPageView.swift`: 125
- `Features/ChatJournal/ChatJournalViews.swift`: 115
- `Features/Community/CommunityPostDetailView.swift`: 93
- `Core/UI/MainShellView.swift`: 81
- `Core/UI/BaseComponents.swift`: 80

### 优化优先级（建议顺序）

- **P0**：把 `CommunityViews.swift` 拆成 `LearnView` / `CommunityPostView` / `CommunityPostDetailView` / `ArticleDetailView` 多文件
- **P0**：把 `HistoricalViews.swift` 拆成 `UserStatusView` / `BasicInfoView` / `SelfTestView` / `SelfTestResultView` 多文件
- **P1**：把 `PracticeViews.swift` 拆成 `PracticeView` 与 `PracticeDetailView`，并抽离计时相关逻辑
- **P1**：把 `OnboardingViews.swift` 拆成 flow / intro / welcome / shared chips
- **P1**：`SharedUI` 中按职责拆分（layout scaffold / nav shell / reusable cards）
- **P2**：补齐 `Domain` 的边界输入测试与回归用例（边界输入 + 报告汇总已完成）

### 验收阈值（建议）

- 任一 Swift 文件尽量控制在 `<= 180` 行
- 单个 View 的本地状态字段尽量控制在 `<= 6`
- PR 粒度控制在“单一主题 + 可回归验证”

### 最新进展（2026-04-24）

- `RuleBasedExtractSignalsUseCase` 已补边界输入测试：空文本、纯标点、超长文本、重复触发词
- `AppViewModel` 已补报告汇总测试：`topSymptoms` / `topTriggers` 的频次排序、空数据、limit 边界
- CI workflow 已增加 `xcresult` 产物上传，失败后可直接下载测试结果排障
- CI workflow 已增加失败摘要写入 Job Summary（可在 PR 检查页直接看失败用例）
- CI 失败摘要逻辑已抽为复用脚本 `.github/summarize_xcresult.py`，降低 workflow 重复维护成本

## Medical Week 1（已完成）

- PR1：医疗安全分级与拦截（`MedicalSafetyGuard` + 注入 `AppViewModel`）
- PR2：对话审计与手机号脱敏（`ConversationAuditor`）
- PR3：RAG 骨架与引用返回（`RAGService` + citations）
- PR4：对话结构化提取 V1（症状/触发因素/生活方式）并持久化 `ConversationInsight`
- PR5：7/30/90 天相关性分析与报告写回（`ReportSnapshot` + `CorrelationAnalyzer` + Reports 页面接入）
- PR6：CI + 评测门禁（已落地：build / unit / ui-smoke / medical-eval）

## Medical Week 2（已完成）

- PR7：已引入 `AuthSession`（手机号会话）并为 UserDefaults 存储增加 user scope
- PR8：已新增 `RemoteJournalRepository` 与 `JournalEntryDTO` 映射层
- PR9：已实现本地优先 + 异步上云的 `RepositoryFacade` 与 `SyncQueue`
- PR10：已补齐同步可靠性与迁移回归测试（重试成功、失败留存、legacy->scoped 迁移）
- PR11：已落地灰度/降级与指标基础（`FeatureFlags` + `RolloutMonitor` + App 层开关入口）

## Medical Week 3（已完成）

- PR12：已切换到真实后端接口形态（`HTTPRemoteJournalAPIClient`，含鉴权头、超时、错误码映射）
- PR13：已把 `SyncQueue` 持久化到本地（按 user scope 恢复 pending/failed 任务）
- PR14：已完成冲突解决策略（按时间戳优先 + 字段级合并）并补回归测试
- PR15：已接入知识库在线更新接口与 RAG 版本标记（`updateKnowledgeBase` + answer/citation version）
- PR16：已落地上线监控面板与告警阈值（同步失败率/远端失败率）

## Medical Week 4（已完成）

- PR17：已落地 token 生命周期管理（`AuthTokenManager`）并接入 `HTTPRemoteJournalAPIClient` 的 401 刷新重试与统一错误码映射（400/401/403/404/409/429/5xx）
- PR18：已新增数据集级医疗门禁测试（召回率、误报率、引用覆盖率）并接入 CI `medical-eval`
- PR19：已扩展 `SyncQueue` 任务状态机（pending/running/retried/failed/succeeded）与历史轨迹持久化，并在设置页提供排障面板
- PR20：已为 `JournalEntry`/DTO 引入 `revision`，冲突合并优先按 revision，再回退到时间戳+字段级合并，并补回放测试
- PR21：已补齐上线检查单与回滚演练文档（`Release-Readiness-Checklist.md`）

