# Step 5 审计：全项目 empty/loading/error 缺口表

> 对应计划：`docs/Audit-First-Execution-Plan.md` 的 Step 5  
> 目标：把状态处理从“局部可用”提升到“系统化、可回归”。

---

## 1. 审计范围

核心路径页面：
- 登录/同意：`WelcomeView`
- AI 对话：`ChatJournalViews`
- 报告：`TrendReportView`、`MedicalListView`
- 社区：`LearnView`
- 呼吸练习：`PracticeView`、`PracticeDetailView`
- 设置与合规：`SettingsView`

检查维度：
- loading
- empty
- error
- retry
- offline/网络失败提示

---

## 2. 页面级缺口表（现状）

| 页面 | loading | empty | error | retry | offline 提示 | 结论 |
|---|---|---|---|---|---|---|
| `WelcomeView` | 无显式 loading | N/A | 弱（登录失败未反馈） | 无 | 无 | **P1** |
| `ChatJournalViews` | 发送中有 `ProgressView` | 输入空文本有 guard | 无显式错误态（AI失败无提示） | 无 | 无 | **P0** |
| `TrendReportView` | 无显式 loading | 有（`sampleCount==0`） | 无 | 无 | 无 | **P1** |
| `MedicalListView` | 无显式 loading | 部分有（症状为空） | 无 | 无 | 无 | **P1** |
| `LearnView` | 刷新依赖系统样式，无页面 loading 骨架 | 有（官方/用户空态文案） | 弱（source=error 但未显式错误块） | 仅下拉刷新 | 无明确离线提示 | **P0** |
| `PracticeView` | 无 | N/A | 无 | 无 | 无 | **P2** |
| `PracticeDetailView` | 会话过程有状态变化 | N/A | 弱（异常无提示） | 无 | 无 | **P2** |
| `SettingsView` | 内部看板为静态读取，无加载态 | 有（告警为空） | 无统一错误反馈 | 无 | 无 | **P2** |

---

## 3. 优先级整改清单

## 3.1 P0（必须先做）

1. `ChatJournalViews` 增加 AI 请求失败态（toast/banner）与“重试发送”按钮
2. `LearnView` 增加“加载中骨架 + 加载失败卡片 + 点击重试”三态
3. 统一网络失败文案组件（用于 AI/社区/报告关键读取路径）

## 3.2 P1（闭环后立即做）

1. `WelcomeView` 登录失败/超时提示与重试
2. `TrendReportView`、`MedicalListView` 增加“加载中”和“数据拉取失败”态
3. 报告页在 empty 状态增加“去记录”快捷按钮，缩短回流路径

## 3.3 P2（体验优化）

1. `Practice` 相关页补统一异常反馈（如计时器中断）— **已实现**：详情页「重新开始」、后台自动暂停、说明文案；列表页离线提示。
2. `Settings` 内部监控区增加刷新与异常提示 — **已实现**：上线监控 / 同步排障 / 运行告警卡片内「刷新」调用 `refreshInternalDiagnosticPanels()`。
3. 加入弱网提示（网络不可用时顶部 banner）— **已实现**：`NWPathMonitor` + `ContentView` 顶部 `safeAreaInset`；社区与练习列表补充离线说明。

---

## 4. 系统化落地建议

- 增加统一页面状态容器：`LoadableStateView`（`loading/empty/error/content`）
- 增加统一重试协议：`RetryableAction`
- 增加统一错误映射：`UserFacingErrorMapper`（技术错误 -> 用户文案）
- 先覆盖 P0 页面，再横向推广到其余页面

---

## 5. Step 5 验收结论

- [x] 已形成页面级缺口总表
- [x] 已按 P0/P1/P2 分组
- [x] 核心路径（AI/社区/报告/登录）已纳入优先级整改清单

下一步：执行 Step 6（AI 安全边界规范）。

