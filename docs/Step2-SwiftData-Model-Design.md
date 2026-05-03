# Step 2 设计：SwiftData 本地模型（含同步字段）

> 对应计划：`docs/Audit-First-Execution-Plan.md` 的 Step 2  
> 前置输入：`docs/Step1-UserDefaults-Audit-and-Migration-Plan.md`  
> 目标：统一本地业务模型，覆盖当前功能与 PRD v2.0，并为后续云端同步保留扩展位。

---

## 1. 设计原则

- **业务优先**：围绕“记录-分析-反馈-练习-社区”主链路建模。
- **离线优先**：本地可完整读写，网络仅做同步增强。
- **同步友好**：每个业务实体统一包含同步字段。
- **可审计**：关键用户动作（同意、删除、导出）可追溯。

---

## 2. 统一基础字段（所有可同步实体）

以下字段建议作为所有业务实体的统一基础字段：

- `id: String`（UUID）
- `userID: String`
- `createdAt: Date`
- `updatedAt: Date`
- `deletedAt: Date?`（软删除）
- `syncStatus: String`（`localOnly` / `pendingUpload` / `synced` / `failed`）
- `syncRevision: Int`（本地版本号，冲突比较）
- `serverID: String?`（服务端主键映射）
- `lastSyncedAt: Date?`

---

## 3. 核心模型设计（满足 Step 2 要求）

## 3.1 UserProfile

用途：用户基础画像、偏好和非敏感配置。

关键字段：
- `id`, `userID`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`, `syncRevision`, `serverID`, `lastSyncedAt`
- `phoneMasked: String?`
- `nickname: String?`
- `birthYear: Int?`
- `menopauseStage: String?`（如 peri/post）
- `preferredReminderTime: String?`
- `guidedConversationEnabled: Bool`

关系：
- `UserProfile (1) -> (N) ConsentRecord`
- `UserProfile (1) -> (N) ContentBookmark`

## 3.2 SymptomRecord

用途：症状主记录，来自手动录入或 AI 提取。

关键字段：
- 基础同步字段（同上）
- `recordDate: Date`
- `symptomType: String`（如 hot_flash/sweat/insomnia）
- `severity: Int?`（1-5）
- `note: String?`
- `source: String`（manual/ai_extracted）
- `sourceConversationID: String?`

关系：
- `SymptomRecord (N) -> (1) AIConversation?`（可选来源）

## 3.3 MoodRecord

用途：情绪记录（独立建模，便于分析趋势）。

关键字段：
- 基础同步字段
- `recordDate: Date`
- `moodLabel: String`（anxious/irritable/calm 等）
- `score: Int?`（1-10）
- `note: String?`
- `source: String`

## 3.4 SleepRecord

用途：睡眠质量与时长。

关键字段：
- 基础同步字段
- `sleepDate: Date`
- `durationMinutes: Int?`
- `qualityScore: Int?`（1-5）
- `nightWakeCount: Int?`
- `note: String?`

## 3.5 HotFlashRecord

用途：潮热事件（高频、事件型数据单独建模）。

关键字段：
- 基础同步字段
- `occurredAt: Date`
- `intensity: Int?`（1-5）
- `durationMinutes: Int?`
- `triggerHint: String?`
- `note: String?`

## 3.6 AIConversation

用途：保存 AI 对话及结构化提取结果索引。

关键字段：
- 基础同步字段
- `startedAt: Date`
- `endedAt: Date?`
- `title: String?`
- `rawTranscript: String`（或分片消息 JSON）
- `riskLevel: String?`（low/medium/high）
- `safetyDecision: String?`
- `extractedSymptomsJSON: String?`
- `extractedTriggersJSON: String?`
- `modelName: String?`
- `knowledgeBaseVersion: String?`

关系：
- `AIConversation (1) -> (N) SymptomRecord`
- `AIConversation (1) -> (N) MoodRecord`

## 3.7 ContentBookmark

用途：社区/知识内容收藏与阅读轨迹。

关键字段：
- 基础同步字段
- `contentID: String`
- `contentType: String`（official/user/article/practice）
- `title: String?`
- `bookmarkedAt: Date`
- `lastReadAt: Date?`
- `readProgress: Double?`（0-1）

## 3.8 BreathingSession

用途：呼吸练习完成记录（替代目前仅计数方式）。

关键字段：
- 基础同步字段
- `sessionType: String`（4-7-8/box 等）
- `plannedDurationSec: Int`
- `actualDurationSec: Int`
- `completed: Bool`
- `completedAt: Date?`
- `source: String`（manual/recommendation）

---

## 4. 合规模型（当前闭环必须）

## 4.1 ConsentRecord

用途：同意记录审计（隐私政策、用户协议、免责声明）。

关键字段：
- 基础同步字段
- `consentType: String`（privacy/user_agreement/medical_disclaimer）
- `version: String`
- `accepted: Bool`
- `acceptedAt: Date?`
- `entryPoint: String`（onboarding/settings）

---

## 5. 模型关系图（文本版）

- `UserProfile (1) -> (N) ConsentRecord`
- `UserProfile (1) -> (N) ContentBookmark`
- `AIConversation (1) -> (N) SymptomRecord`
- `AIConversation (1) -> (N) MoodRecord`
- 其余记录（`SleepRecord`、`HotFlashRecord`、`BreathingSession`）直接归属 `userID`

说明：
- 关系以“弱耦合 + 外键字符串”为主，降低本地迁移复杂度。
- 高并发事件（潮热、练习）避免深层嵌套关系。

---

## 6. 与现有数据结构的映射

| 现有结构 | 新模型映射 |
|---|---|
| `JournalEntry` + `ExtractedData` | 拆分映射到 `AIConversation` + `SymptomRecord`/`MoodRecord`/`SleepRecord`（按提取能力逐步落地） |
| `ConversationInsight` | 主体并入 `AIConversation`，可保留轻量索引表（可选） |
| `ReportSnapshot` | 短期仍可计算生成，后续可增加 `ReportArtifact` 模型（Step 4/6 后决定） |
| `practiceTotalSessions`/`practiceLastCompletedAt` | 迁移为 `BreathingSession` 事件记录后实时聚合 |
| `SyncQueue` 三类数据 | 迁移为独立同步任务表（Step 7 与后端方案联动细化） |

---

## 7. 迁移与实现建议（仅设计层）

1. 先建 SwiftData schema（不切读写）
2. 做一次性迁移器：`UserDefaults -> SwiftData`
3. 用 repository 门面切换到“SwiftData 主读写”
4. 保留一个版本的 fallback 只读
5. 完成后清理业务 key，仅保留配置 key

---

## 8. Step 2 验收结论

- [x] 覆盖要求中的 9 个核心模型（`UserProfile`、`SymptomRecord`、`MoodRecord`、`SleepRecord`、`HotFlashRecord`、`AIConversation`、`ContentBookmark`、`BreathingSession`、`ConsentRecord`）
- [x] 每个模型可落地字段已定义（含类型语义）
- [x] 统一同步字段已定义（`id/createdAt/updatedAt/deletedAt/syncStatus` 等）
- [x] 模型关系已明确，满足 PRD v2.0 当前闭环

下一步：执行 Step 3（删除/导出/注销功能审计与落地方案）。

