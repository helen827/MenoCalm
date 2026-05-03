# Step 8 定义：埋点事件字典（首版定稿）

> 对应计划：`docs/Audit-First-Execution-Plan.md` 的 Step 8  
> 目标：先统一统计口径，再进入埋点实现，避免后续反复改名改口径。

---

## 1. 命名与通用规范

- 事件命名：`模块_动作_结果`（小写下划线）
- 参数命名：`camelCase`
- 所有事件必须包含：
  - `userID`
  - `sessionID`
  - `eventTime`
  - `appVersion`
  - `platform`（ios）
  - `networkType`

---

## 2. 核心指标与事件映射

## 2.1 onboarding 完成率

- `onboarding_viewed`
- `onboarding_disclaimer_toggled`
- `onboarding_completed`

口径：
- 完成率 = `onboarding_completed` 用户数 / `onboarding_viewed` 用户数

## 2.2 记录症状人数

- `journal_entry_created`
- `symptom_record_created`

口径：
- 记录症状人数 = 统计周期内至少触发一次 `symptom_record_created` 的去重用户数

## 2.3 连续记录天数

- `journal_entry_created`（含 `entryDate`）

口径：
- 基于 `entryDate` 去重后计算最长连续天数

## 2.4 AI 问答使用次数

- `chat_message_sent`
- `chat_response_generated`
- `chat_safety_blocked`

口径：
- 使用次数 = `chat_message_sent` 次数
- 安全拦截率 = `chat_safety_blocked` / `chat_message_sent`

## 2.5 呼吸练习完成率

- `breathing_session_started`
- `breathing_session_completed`
- `breathing_session_abandoned`

口径：
- 完成率 = `breathing_session_completed` / `breathing_session_started`

## 2.6 内容阅读完成率

- `community_content_opened`
- `community_content_read_progress`
- `community_content_completed`

口径：
- 完成率 = `community_content_completed` / `community_content_opened`

## 2.7 用户退出漏斗位置

- `funnel_step_reached`（stepName）
- `funnel_step_exited`（stepName, reason）

建议 stepName：
- `welcome`
- `chat_first_message`
- `report_first_view`
- `practice_first_complete`
- `community_first_open`

---

## 3. 关键事件字典（首批）

| 事件名 | 触发时机 | 关键参数 | 说明 |
|---|---|---|---|
| `onboarding_viewed` | 欢迎页首次展示 | `entrySource` | 漏斗起点 |
| `onboarding_completed` | 用户进入主壳层 | `acceptDisclaimer` | onboarding 完成 |
| `login_succeeded` | 登录成功 | `authMethod` | 账号成功 |
| `login_failed` | 登录失败 | `errorCode` | 登录失败归因 |
| `chat_message_sent` | 用户发送消息 | `messageLength` | AI 使用基础事件 |
| `chat_response_generated` | AI 响应完成 | `latencyMs`,`riskLevel` | 响应质量与时延 |
| `chat_safety_blocked` | 高风险被拦截 | `riskLevel`,`ruleID` | 安全边界有效性 |
| `journal_entry_created` | 记录成功 | `source` | 手动/AI 提取来源 |
| `report_snapshot_viewed` | 报告页打开 | `rangeDays` | 报告使用 |
| `breathing_session_started` | 开始练习 | `sessionType`,`plannedDurationSec` | 练习启动 |
| `breathing_session_completed` | 完成练习 | `sessionType`,`actualDurationSec` | 练习完成 |
| `community_feed_refreshed` | 社区刷新完成 | `source`,`itemCount` | 内容供给健康度 |
| `community_post_submitted` | 发布成功 | `hasImage` | UGC 生产 |
| `settings_export_requested` | 点导出并确认 | `dataScopeVersion` | 合规动作 |
| `settings_delete_requested` | 点删除并确认 | `deleteRemote` | 合规动作 |
| `settings_account_deactivated` | 注销成功 | `reason` | 生命周期终点 |

---

## 4. 看板建议（首版）

- 增长：onboarding 完成率、7日留存、首日激活
- 核心使用：AI 日活消息数、报告查看率、练习完成率
- 质量：AI 响应时延 P50/P95、错误率、重试率
- 安全：高风险拦截率、高风险漏检数
- 合规：导出请求成功率、删除请求成功率

---

## 5. 归因维度（统一）

- 用户维度：新用户/老用户、是否完成同意流
- 场景维度：chat/report/practice/community
- 时间维度：日/周/月
- 风险维度：`riskLevel`（low/medium/high）
- 网络维度：wifi/cellular/offline

---

## 6. Step 8 验收结论

- [x] 事件命名规则与通用字段已统一
- [x] 指标到事件映射已完成
- [x] 首批关键事件字典已定稿
- [x] 看板指标与归因维度已定义

本轮 8 个步骤已全部完成文档交付。

