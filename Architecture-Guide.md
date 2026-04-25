# 潮安 Architecture Guide

## 1. 目标

本项目当前目标是：

- 在不改变现有原型行为的前提下，建立可维护、可扩展的代码结构；
- 将 UI、业务逻辑、数据访问分层，减少耦合；
- 为后续新增功能（例如更复杂报告、账号体系、服务端同步）保留清晰扩展点。

---

## 2. 架构原则

- **单向依赖**：`Features` 依赖 `Domain` 与 `Core`；`Data` 实现 `Domain` 协议；`App` 负责装配。
- **职责分离**：
  - 业务规则放在 `Domain/UseCases`
  - 存储实现放在 `Data/Repositories`
  - 全局状态与路由放在 `App`
  - 可复用 UI 组件放在 `Core/UI`
- **渐进重构**：优先文件级拆分（同一 target 内），后续再视团队规模升级到多模块。

---

## 3. 当前目录结构（已对齐）

```text
潮安/
  App/
    AppEntry.swift
    ContentView.swift
    AppRouter.swift
    AppState.swift

  Core/
    DesignSystem/
      Theme.swift
    UI/
      SharedUI.swift

  Domain/
    Entities/
      Models.swift
    Repositories/
      JournalRepositoryProtocol.swift
    UseCases/
      ExtractSignalsUseCase.swift

  Data/
    Repositories/
      UserDefaultsJournalRepository.swift

  Features/
    ChatJournal/
      ChatJournalViews.swift
    Community/
      CommunityViews.swift
    Historical/
      HistoricalViews.swift
    Onboarding/
      OnboardingViews.swift
    Practice/
      PracticeViews.swift
    Profile/
      ProfileViews.swift
    Reports/
      ReportsViews.swift
```

---

## 4. 关键职责说明

### App

- `AppEntry.swift`：应用入口。
- `ContentView.swift`：根视图，切换 onboarding 与主流程。
- `AppRouter.swift`：全局导航路径与 Tab 管理。
- `AppState.swift`：`AppViewModel`（会话态、记录保存入口）与路由枚举。

### Domain

- `Models.swift`：核心实体（`JournalEntry`、`ExtractedData` 等）。
- `ExtractSignalsUseCase.swift`：文本信号提取业务规则（可替换为更高级策略）。
- `JournalRepositoryProtocol.swift`：数据访问抽象。

### Data

- `UserDefaultsJournalRepository.swift`：`JournalRepositoryProtocol` 的默认本地实现。

### Core

- `Theme.swift`：设计 token（颜色、间距、字体）。
- `SharedUI.swift`：全局复用组件（`PageScaffold`、`FrostedCard`、`CATabBar` 等）。

### Features

- 每个功能域仅维护本域页面与交互（例如 `Practice`、`Reports`、`Community`）。
- 功能页面通过 `EnvironmentObject` 使用 `AppViewModel` 与 `AppRouter`，避免重复状态定义。

---

## 5. 依赖关系（约束）

- `Features -> App/Core/Domain`
- `App -> Domain/Data/Core`
- `Data -> Domain`
- `Domain` 不依赖 `App/Features/Data`
- `Core` 不依赖业务语义对象

---

## 6. 下一步演进建议

当前已完成：

1. `Domain` 关键用例已补齐（边界输入、报告聚合、医疗安全与回归集）。
2. 主要功能文件已拆分并目录归位到 `App/Core/Domain/Data/Features`。
3. CI 门禁已落地（build + unit + UI smoke + medical eval）。
4. `AuthSession` + user scope 数据隔离已落地（本地存储按用户分区）。
5. 云端基础层已完成（`RemoteJournalRepository` / DTO / `RepositoryFacade` / `SyncQueue`）。
6. 灰度和降级基础已落地（`FeatureFlags` + `RolloutMonitor`）。
7. 远端接口已升级到 HTTP 形态（鉴权头、超时、状态码映射，默认可降级到本地）。
8. 同步队列已支持本地持久化恢复（`SyncQueue` 在冷启动后可继续执行 pending 任务）。
9. 本地/云端冲突已引入显式合并策略（时间戳优先 + 字段级 union，避免数据覆盖丢失）。
10. RAG 已支持知识库热更新与版本标记（回复与引用均带 `knowledgeBaseVersion`）。
11. 监控面板与阈值告警已落地（同步失败率、远端读取失败率）。

Week 4 已完成：

1. 真实后端鉴权闭环已打通：引入 `AuthTokenManager`，支持 access token 过期刷新，并在 `HTTPRemoteJournalAPIClient` 增加 401 自动刷新重试与统一状态码映射。
2. 医疗可靠性门禁已升级到数据集级：新增召回率/误报率/引用覆盖率统计阈值测试，并接入 CI `medical-eval`。
3. `SyncQueue` 已支持任务生命周期可追踪：状态覆盖 pending/running/retried/failed/succeeded，并持久化任务轨迹用于排障。
4. 冲突策略已引入 `revision`：同一条数据冲突时优先高 revision，无法判定时回退时间戳+字段级合并。
5. 已形成上线检查单与回滚演练模板：覆盖灰度开关、阈值基线、故障处理流程和预发布演练步骤。

---

## 7. 近期优化跟踪重点

- **文件体量热点**：`CommunityViews.swift`、`HistoricalViews.swift`、`OnboardingViews.swift`、`PracticeViews.swift`、`SharedUI.swift`。
- **优先动作**：先拆 `Community` 与 `Historical`（风险低、收益高），再拆 `Practice` 与 `Onboarding`。
- **验收口径**：
  - 单文件尽量 `<= 300` 行；
  - 每次 PR 只做一类拆分；
  - 每次拆分后都执行 build + smoke test，保证“结构变、行为不变”。

