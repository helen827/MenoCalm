# Swift Package 模块化评估（草案）

## 目标

在不影响交付速度的前提下，将 `潮安` 主工程拆为可独立编译、依赖清晰的模块，降低 Features 与 Data 的耦合。

## 候选模块

| 模块 | 内容 | 依赖 |
|------|------|------|
| `MenoCalmDesignSystem` | `Theme.swift`、`BaseComponents`、`PageScaffold`、共享色板 | SwiftUI |
| `MenoCalmDomain` | `Models`、用例协议、医疗安全/RAG 纯逻辑（无 UI） | Foundation |
| `MenoCalmData` | Repository 实现、DTO、`SyncQueue`、API 客户端 | Domain、Foundation |
| `MenoCalmFeatures` | 各 Feature 视图 | DesignSystem、Domain、Data（或通过协议注入） |
| `MenoCalmApp` | `AppViewModel`、`ContentView`、入口 | Features、Data |

## 迁移风险

- **循环依赖**：`AppViewModel` 同时依赖多 Feature 时，需将路由与 DI 保留在 App 目标。
- **资源文件**：`community_feed.json`、Assets 需明确归属 `App` 或独立 `Resources` 包。
- **测试**：`@testable import` 需改为对各模块的 target 依赖。

## 建议 PoC 顺序

1. 抽出 `MenoCalmDesignSystem`（无业务依赖，风险最低）。
2. 抽出 `MenoCalmDomain` + 现有单测。
3. 再动 `Data` / `Features`。

## 验收

- 主 App target 行数下降；各 Package 可单独 `swift build`（若启用 SPM）。
- CI 仍全绿。
