# 潮安 Refactor Implementation Checklist

## 当前行动清单（精简版）

发布验收（PRD §7 对齐）：见 [ops/Release-Acceptance-Checklist.md](ops/Release-Acceptance-Checklist.md)  
生产联调：[ops/Production-Backend-Runbook.md](ops/Production-Backend-Runbook.md)｜上架：[ops/App-Store-Release-Checklist.md](ops/App-Store-Release-Checklist.md)｜SPM 评估：[docs/SPM-Modularization-Assessment.md](docs/SPM-Modularization-Assessment.md)｜社区口径：[docs/Community-Content-Policy.md](docs/Community-Content-Policy.md)

状态说明：`Todo` / `Doing` / `Blocked` / `Done`

- P0｜继续组件化高频页面（`ChatJournal` / `Reports` / `Profile`）｜Status：Doing｜验收：单文件尽量 `<= 180` 行，状态字段 `<= 6`
- P1｜`SharedUI` 按职责拆分（layout / nav / cards）｜Status：Doing｜验收：页面可复用组件占比提升，重复 UI 逻辑减少
- P1｜评估 Swift Package 模块化切入点｜Status：Todo｜验收：给出 1 份候选模块清单（边界、依赖、迁移风险）
- P1｜维护 CI 四道门禁稳定性（`build` / `unit-tests` / `ui-smoke` / `medical-eval`）｜Status：Doing｜验收：主分支连续稳定，无门禁退化
- P2｜医疗评测数据集持续扩容（表达变体/误报漏报样本）｜Status：Doing｜验收：门禁统计波动收敛，回归集持续更新

## 本周建议节奏

- Day 1-2：完成 `ChatJournal` / `Reports` 组件拆分第一轮
- Day 3：完成 `SharedUI` 分层并清理重复样式
- Day 4：输出 SPM 模块化评估草案（候选模块 + 风险）
- Day 5：跑全量 CI + 回归，收口文档与遗留问题

## 历史归档（已完成）

说明：以下内容为历史执行记录，默认不再作为日常跟进清单。

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

## Phase 3: 可选增强（部分完成）

- 已完成：补充 Domain 单测（报告汇总、边界输入）
- 进行中：Features 内继续组件化，控制单文件体积
- 已完成：CI 自动门禁（`build` / `unit-tests` / `ui-smoke` / `medical-eval`）
- 待评估：Swift Package 模块化的切入点

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

## 体验优化任务池（已完成）

状态说明：`Todo` / `Doing` / `Blocked` / `Done`

### Epic A：AI 对话体验升级（P0）

- A1 意图澄清首轮（引导式提问）｜Status：Done｜验收：用户首句后优先出现 1-2 个澄清问题（目标/场景/期望）
- A2 回复语气升级（同理 + 结构化建议）｜Status：Done｜验收：回复模板包含「共情句 + 可执行建议 + 下一步问题」
- A3 多轮上下文记忆（短会话窗口）｜Status：Done｜验收：同一会话内可引用上一轮信息，减少重复提问
- A4 对话策略 AB 开关｜Status：Done｜验收：可在配置中切换“直接建议”与“先澄清后建议”策略
- A5 对话体验回归测试集｜Status：Done｜验收：新增 10+ 组真实表达回归用例，防止语气/意图识别退化

### Epic B：UIUX 系统优化（P1）

- B1 AI 对话页改版（信息层级 + 输入区反馈）｜Status：Done｜验收：主路径可读性提升，输入状态清晰
- B2 报告页可视化优化（重点结论前置）｜Status：Done｜验收：用户 10 秒内可读出“本周关键变化”
- B3 设置页信息架构重排｜Status：Done｜验收：高频项（隐私、免责声明、告警）点击层级不超过两层
- B4 全局交互细节统一（按钮/弹窗/加载/空态）｜Status：Done｜验收：核心页面控件行为与反馈一致
- B5 文案风格统一（医疗场景语气规范）｜Status：Done｜验收：完成文案规范卡并覆盖高频页面

### 近期执行顺序（建议）

- Sprint 1：A1 + A2 + B1（先解决“冷冰冰”和主路径体验）
- Sprint 2：A3 + A5 + B2（稳定多轮体验与报告可读性）
- Sprint 3：A4 + B3 + B4 + B5（策略开关与全局一致性收口）

## 真实上线收口清单（已完成，进入持续维护）

### P0（上线前必须完成，已完成）

- 已完成：真实后端鉴权接入（替换本地模拟 token 流程，登录/刷新/失效回收与 401/403/429/5xx 联调完成）。
- 已完成：生产环境配置分层（dev/staging/prod 配置隔离落地，避免测试配置误入生产包）。
- 已完成：线上监控与告警接入（Crash + 网络错误 + 关键业务失败率监控与告警通知链路已接入）。
- 已完成：隐私与医疗合规文档定版（隐私政策、用户协议、医疗免责声明、脱敏策略已固化）。
- 已完成：灰度发布与回滚演练（已完成真实演练并留存记录）。

### P1（首发后 1-2 周完成，已完成）

- 已完成：AI 对话体验升级（先意图澄清再建议，提升意图理解与同理表达）。
- 已完成：全局 UIUX 打磨（AI 对话页、报告页、设置页等高频路径统一）。
- 已完成：医疗评测数据集扩容（误报/漏报/表达变体样本扩充，门禁统计更稳定）。

## 真实上线执行计划（14 天倒排）

### 第 1 周：技术与联调闭环

- Day 1-2：后端鉴权联调（登录/刷新/失效），补齐失败路径测试与日志字段。
- Day 3：环境配置分层落地（dev/staging/prod），完成配置校验脚本。
- Day 4：接入线上监控 SDK 与关键埋点（崩溃、远端失败率、同步失败率、关键页面异常）。
- Day 5：告警规则与通知通道联调（阈值、通知对象、升级策略）。
- Day 6-7：staging 回归（CI 全量 + 人工冒烟 + 灰度开关验证）。

### 第 2 周：发布准备与演练

- Day 8-9：合规文档终版确认（隐私政策/免责声明/用户协议）与应用内入口校验。
- Day 10：首轮灰度发布演练（小流量），观察指标与错误码分布。
- Day 11：回滚演练（人工触发），验证故障处置 SOP 与恢复时长。
- Day 12：根据演练结果修复阻塞项，复测关键链路。
- Day 13：Go/No-Go 评审（CI、监控、合规、回滚记录四项齐备）。
- Day 14：正式放量上线（分阶段扩大），持续监控并记录首日数据。

### Go/No-Go 验收门槛

- CI 四道门禁全绿：`build` / `unit-tests` / `ui-smoke` / `medical-eval`。
- 真实环境下关键指标连续稳定（同步失败率、远端读取失败率、数据集门禁指标）。
- 无 P0 blocker；回滚演练通过且有完整记录。

## 真实上线执行看板（可勾选）

状态说明：`Todo` / `Doing` / `Blocked` / `Done`

- Day 1：对接真实登录 API 与 token 获取链路｜Owner：iOS + Backend｜Status：Done｜验收：真机/模拟器登录成功，token 可写入本地安全存储
- Day 2：完成 refresh token 与失效回收联调｜Owner：iOS + Backend｜Status：Done｜验收：401 后可刷新重试；刷新失败会清理会话并提示重登
- Day 3：落地 dev/staging/prod 配置隔离｜Owner：iOS｜Status：Done｜验收：三套环境可切换，且产物不会误指向测试环境
- Day 4：接入 Crash 与关键错误监控埋点｜Owner：iOS + Ops｜Status：Done｜验收：可看到崩溃、远端失败率、同步失败率、关键页面异常
- Day 5：配置告警规则与通知链路｜Owner：Ops｜Status：Done｜验收：阈值触发后可在通知渠道收到告警并定位到指标
- Day 6：执行 staging 全量 CI 回归｜Owner：QA + iOS｜Status：Done｜验收：`build`/`unit-tests`/`ui-smoke`/`medical-eval` 全绿
- Day 7：执行人工冒烟与灰度开关验证｜Owner：QA + PM｜Status：Done｜验收：主流程无阻塞，云读/云写/降级开关符合预期
- Day 8：完成隐私政策/用户协议/医疗免责声明终版｜Owner：PM + Legal｜Status：Done｜验收：文案冻结并通过法务确认
- Day 9：应用内合规入口与文案一致性校验｜Owner：iOS + QA｜Status：Done｜验收：设置页与首次进入链路均可访问最新合规文案
- Day 10：首轮小流量灰度演练｜Owner：Ops + iOS｜Status：Done｜验收：灰度用户稳定，错误码分布与失败率在阈值内
- Day 11：执行回滚演练（人工触发）｜Owner：Ops + iOS｜Status：Done｜验收：15 分钟内完成回滚并恢复可用
- Day 12：修复演练暴露问题并复测｜Owner：iOS + QA｜Status：Done｜验收：阻塞问题清零，回归通过
- Day 13：Go/No-Go 评审｜Owner：PM + Tech Lead + Ops｜Status：Done｜验收：CI、监控、合规、回滚记录四项齐备且无 P0 阻塞
- Day 14：分阶段正式放量上线｜Owner：Ops + PM｜Status：Done｜验收：首日监控稳定，发布复盘与数据记录完成