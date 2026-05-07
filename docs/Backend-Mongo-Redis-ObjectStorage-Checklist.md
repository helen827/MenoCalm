# 后端实施清单（MongoDB + Redis + 对象存储）

> 目的：作为后端开发过程中的长期执行基线，确保每次迭代都对齐“要做什么、谁来做、做到什么算完成”。

## 1. 当前技术路线（已确认）

- 主后端工程：`menocalm-api/`
- 主业务数据库：`MongoDB`
- 缓存与短期状态：`Redis`
- 媒体文件存储：`对象存储`（S3 / OSS / COS / MinIO，S3 兼容优先）

---

## 2. 职责分工

## 2.1 你需要准备（业务/环境侧）

- 提供运行环境配置：
  - `MONGODB_URI`
  - `REDIS_URL`
  - `JWT_SECRET`（至少 32 位）
  - `S3_ENDPOINT`（AWS 可为空）
  - `S3_BUCKET`
  - `S3_ACCESS_KEY`
  - `S3_SECRET_KEY`
  - `S3_REGION`
- 确认对象存储厂商（S3/OSS/COS/MinIO）
- 确认 Redis 首期用途：验证码 + 登录限流（默认启用）
- 确认“是否先用占位符配置开发，后续替换真实密钥”

## 2.2 我需要完成（后端研发侧）

- MongoDB：
  - 完成核心集合建模、索引、唯一约束、TTL 策略
  - 保证登录、journal、社区、媒体元数据可持久化
- Redis：
  - 实现验证码缓存、登录限流、短期会话控制
  - 实现过期策略、错误回退策略
- 对象存储：
  - 实现上传 key 生成与签名（或服务端中转上传）
  - 存储媒体元数据到 Mongo
  - 实现访问控制（公开/私有）与基本安全校验
- 工程质量：
  - 统一错误响应结构
  - 单元测试 + 关键集成测试
  - 阶段性测试脚本与文档

---

## 3. 数据设计范围（首期）

## 3.1 MongoDB（业务主库）

- `users`：用户主档
- `auth_identities`：登录身份映射（手机号/微信）
- `refresh_tokens`：刷新令牌哈希与过期信息
- `journal_entries`：用户记录
- `conversation_messages`：AI 对话消息
- `conversation_insights`：分析结果
- `community_posts`：社区帖子
- `community_comments`：社区评论
- `community_media`：媒体元数据（文件 URL、类型、大小、hash、上传者）
- `community_moderation`：审核结果

## 3.2 Redis（状态与策略）

- 验证码：
  - `auth:sms:code:{phone}`
- 限流：
  - `ratelimit:login:phone:{phone}`
  - `ratelimit:login:ip:{ip}`
- 短期会话（如需要）：
  - `session:challenge:{id}`

## 3.3 对象存储（文件内容）

- Bucket 建议分区：
  - `community/images/`
  - `community/videos/`
  - `avatars/`
- Mongo 只保存元数据，不保存二进制内容

---

## 4. 分阶段实施

## 阶段 A（基础可用）

- 接通 MongoDB + Redis + 对象存储配置
- 增加启动时配置校验
- 增加 `/actuator/health` 依赖探针（mongo/redis）

**完成标准**
- 服务启动通过
- 三类依赖连通检查通过

## 阶段 B（认证与安全）

- 手机号登录、refresh token 流程稳定
- Redis 验证码与限流生效
- 统一错误响应（含 requestId）

**完成标准**
- 登录链路在并发下可用
- 限流命中返回稳定错误码

## 阶段 C（内容与文件）

- Journal 与社区内容使用真实 Mongo 数据
- 媒体上传链路走对象存储
- 媒体元数据可追踪可回收

**完成标准**
- 发帖可引用真实上传媒体
- 删除内容时可触发媒体回收策略（或标记清理）

## 阶段 D（质量与发布）

- 补齐单元测试与关键集成测试
- 建立阶段性测试命令与 CI 门禁

**完成标准**
- `mvn test` 稳定通过
- 核心接口有自动化回归

---

## 5. 阶段性测试清单

## 5.1 单元测试（必须）

- AuthService：登录/刷新/token 轮换/异常分支
- JournalService：权限校验/去重/覆盖写入
- CommunityService：数据存在/缺失行为
- Redis 策略：验证码 TTL、限流窗口、计数边界
- 对象存储服务：key 生成、签名有效期、元数据写入

## 5.2 集成测试（必须）

- Mongo 持久化读写测试
- Redis 限流与验证码流程测试
- 上传接口 -> 元数据落库测试
- 鉴权失败/越权访问测试

## 5.3 回归测试（每次迭代）

- 登录 -> 刷新 token -> 拉取 journal -> 写入 journal
- 社区 feed -> 发帖 -> 评论 -> 媒体引用
- 错误响应结构一致性：`code/message/requestId/path/timestamp`

---

## 6. 配置模板（占位符）

```bash
SPRING_PROFILES_ACTIVE=dev
MONGODB_URI=mongodb://127.0.0.1:27017/livemore
REDIS_URL=redis://127.0.0.1:6379
JWT_SECRET=replace-with-strong-secret-at-least-32-chars

S3_ENDPOINT=http://127.0.0.1:9000
S3_BUCKET=live-more-assets
S3_ACCESS_KEY=replace-access-key
S3_SECRET_KEY=replace-secret-key
S3_REGION=us-east-1
```

---

## 7. 每次开发前检查（Keep In Mind）

- 当前改动是否引入“假数据”或“临时逻辑”？
- 是否破坏 iOS 已有接口契约？
- 是否补了对应单元测试/集成测试？
- 错误响应是否仍保持统一结构？
- 是否能通过阶段性测试命令？

---

## 8. 下一步执行（立即）

- 你提供：对象存储类型 + 关键配置值（可先占位符）
- 我执行：先落地阶段 A/B（配置校验 + Redis 登录限流 + 测试）
- 通过后进入阶段 C（媒体上传与元数据）

