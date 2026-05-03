# 审计优先版执行 Plan（单人执行，不按时间拆分）

> 目标：先把项目从“能跑”升级到“可评估、可收口、可上线决策”状态。  
> 范围：本轮先做审计、模型设计、方案定义、验收口径，不直接新增功能。

## 执行进度

- Step 1 — UserDefaults 全量盘点与迁移方案（详见：`docs/Step1-UserDefaults-Audit-and-Migration-Plan.md`）
- Step 2 — 本地数据库模型设计（SwiftData）（详见：`docs/Step2-SwiftData-Model-Design.md`）
- Step 3 — 删除/导出/注销功能审计与落地方案（详见：`docs/Step3-Delete-Export-Logout-Audit-and-Implementation-Plan.md`）
- Step 4 — 最小闭环验收定义（单人执行版）（详见：`docs/Step4-Minimum-Closed-Loop-Acceptance.md`）
- Step 5 — 全项目 empty/loading/error 缺口表（详见：`docs/Step5-Empty-Loading-Error-Gap-Table.md`）
- Step 6 — AI 安全边界规范（详见：`docs/Step6-AI-Safety-Boundary-Spec.md`）
- Step 7 — 后端方案决策稿（Supabase + SwiftData）（详见：`docs/Step7-Supabase-SwiftData-Architecture-Decision.md`）
- Step 8 — 埋点方案（事件字典）（详见：`docs/Step8-Analytics-Event-Dictionary.md`）

---

## Step 1 — UserDefaults 全量盘点与迁移方案

### 目标

把当前所有本地存储点梳理清楚，分成“保留”与“迁移”。

### 产出

- UserDefaults 使用清单（文件、key、数据内容、风险）
- 分类结果：
  - 继续保留：设置/开关/轻量配置
  - 迁移数据库：业务核心数据（记录、报告、会话、收藏等）
- 迁移策略：双写期、回填、回滚、校验点

### 验收

- 每个 key 都有明确归属和结论
- 无灰区项（“待定”项必须有决策人和结论时间）

---

## Step 2 — 本地数据库模型设计（SwiftData）

### 目标

先定数据模型，再改代码。

### 产出

至少覆盖以下模型并给出字段表（类型/必填/默认值）：

- UserProfile
- SymptomRecord
- MoodRecord
- SleepRecord
- HotFlashRecord
- AIConversation
- ContentBookmark
- BreathingSession
- ConsentRecord

并统一补齐同步字段：

- id
- createdAt
- updatedAt
- deletedAt
- syncStatus

### 验收

- 模型覆盖当前功能与 PRD v2.0
- 模型关系（1-1 / 1-N）清晰，无冲突

---

## Step 3 — 删除/导出/注销功能审计与落地方案

### 目标

确认哪些只是 UI 占位，补齐真实动作清单。

### 产出

- 三个入口审计：
  1. 当前入口位置
  2. 当前实际逻辑
  3. 缺失逻辑
- 真实实现方案（前端动作 + 服务端接口 + 本地清理 + 用户反馈）
- 数据范围定义（导出/删除包含哪些数据）

### 验收

- 每个入口都能映射完整链路：UI -> 业务 -> 存储 -> 回执

---

## Step 4 — 最小闭环验收定义（单人执行版）

### 目标

定义“可上线最小闭环”，作为唯一验收门槛。

### 建议闭环

登录 -> AI记录 -> 结构化提取 -> 报告生成 -> 呼吸练习完成记录 -> 社区浏览 -> 合规入口可达

### 产出

- 最小闭环验收清单（逐项可勾选）
- 失败判定标准（Fail Criteria）

### 验收

- 任何人按清单执行，结论一致

---

## Step 5 — 全项目 empty/loading/error 缺口表

### 目标

把状态处理从“局部有”变成“系统化”。

### 产出

- 页面级审计表（数据读取、AI回答、内容加载、登录状态相关页）
- 每页 5 项检查：
  1. loading
  2. empty
  3. error
  4. retry
  5. offline/网络失败提示
- 优先级分组：P0/P1/P2

### 验收

- 核心路径（AI/报告/社区/登录）全部纳入 P0/P1 整改

---

## Step 6 — AI 安全边界规范

### 目标

把“能回答”提升为“可控回答”。

### 产出

- 安全边界文档：
  - 禁止输出
  - 高风险拦截条件
  - 必须出现的就医引导模板
- 风险分级响应策略（low/medium/high）
- 回归样例集（正/反例）

### 验收

- 边界规则可测试、可回归，不是仅文案描述

---

## Step 7 — 后端方案决策稿（Supabase + SwiftData）

### 目标

先做架构决策，再开发。

### 产出

- 方案评估：为何选择 Supabase + SwiftData
- 架构草图：Auth、DB、Storage、Edge Functions、同步策略
- 风险分析：合规、性能、离线冲突、成本
- 决策结论：可做/不可做 + 前置条件
- **（已扩展）** 与现有 iOS REST 契约对照、Postgres v1 表草案、RLS 模板、路径 A（BFF/Edge 兼容）/ 路径 B（直连 PostgREST）分阶段策略、Edge Functions 清单与后端执行勾选表 — 见 `docs/Step7-Supabase-SwiftData-Architecture-Decision.md` §10–§19

### 验收

- 能明确回答“为什么现在选它，而不是其他方案”
- 能按对照表在 Supabase 上实现或适配当前 `/api/v1/*` 与 Journal DTO，而不依赖口头约定

---

## Step 8 — 埋点方案（事件字典）

### 目标

可衡量产品行为（先定义口径，不先上代码）。

### 指标范围

- onboarding 完成率
- 记录症状人数
- 连续记录天数
- AI 问答使用次数
- 呼吸练习完成率
- 内容阅读完成率
- 用户在哪一步退出

### 产出

- 事件字典：事件名、触发时机、参数、口径
- 看板指标定义与归因维度

### 验收

- 事件命名和统计口径一次定稿，后续实现不反复

---

## 最终交付包（本轮）

1. 存储迁移方案
2. SwiftData 模型设计文档
3. 删除/导出/注销闭环方案
4. 最小闭环验收标准
5. 状态缺口优先级表
6. AI 安全边界规范
7. Supabase 架构决策稿
8. 埋点事件字典