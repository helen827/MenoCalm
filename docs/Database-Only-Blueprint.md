# 数据库蓝图（仅数据库）

> 只聚焦数据库层，用于中国区 App 上线的长期方案。结论先行：推荐 `MySQL + Redis + OSS/COS`，并保留当前 `Mongo` 作为过渡路径。

## 1. 生产推荐（中国区）

- 主库：`MySQL 8.0`（或 TiDB 兼容 MySQL）
- 缓存与限流：`Redis`
- 对象存储：`阿里云 OSS` 或 `腾讯云 COS`（S3 兼容接口）
- 当前状态：项目已落地 `Mongo`，可短期继续用；中期建议迁移主业务到 MySQL

---

## 2. 目标数据库架构（MySQL + Redis + 对象存储）

## 2.1 MySQL（主业务数据）

建议先建这些核心表：

- `users`：用户主档（手机号、昵称、状态、注册时间）
- `auth_identities`：多登录身份（phone/wechat openid/unionid）
- `refresh_tokens`：刷新令牌哈希（可设置过期清理）
- `journal_entries`：用户记录与结构化提取结果
- `conversation_messages`：AI 对话逐条消息
- `conversation_insights`：分析结果（症状/诱因/风险）
- `community_posts`：社区帖子
- `community_comments`：评论/回复
- `community_media`：媒体元数据（对象存储 key）
- `community_moderation`：审核与举报处理

关键索引（首期）：

- `users(phone_e164)` 唯一
- `auth_identities(provider, provider_uid)` 唯一
- `refresh_tokens(token_hash)` 唯一
- `journal_entries(user_id, entry_id)` 唯一
- `journal_entries(user_id, created_at)` 普通索引
- `conversation_messages(conversation_id, created_at)` 普通索引
- `community_posts(status, created_at)` 普通索引
- `community_media(bucket, object_key)` 唯一

## 2.2 Redis（短期状态）

- 验证码：
  - Key：`auth:sms:code:{phone}`
  - TTL：`300s`
- 登录限流：
  - `ratelimit:login:phone:{phone}`
  - `ratelimit:login:ip:{ip}`
  - TTL：`300s`（默认）
- 防重放（可选）：
  - `auth:refresh:used:{tokenHash}`
  - TTL：与 token 剩余有效期一致

## 2.3 对象存储（文件内容）

- Bucket 示例：`live-more-assets`
- Key 规范：
  - `community/images/{yyyy}/{mm}/{userId}/{uuid}.jpg`
  - `community/videos/{yyyy}/{mm}/{userId}/{uuid}.mp4`
  - `avatars/{userId}/{uuid}.png`
- Mongo/MySQL 只存元数据，不存二进制
- 访问策略：
  - 社区图可 public（或 CDN）
  - 私有文件通过签名 URL

---

## 3. 当前 Mongo 过渡模型（已实现）

当前后端已落地如下 Mongo 集合，可作为迁移前过渡：

- `users`
- `refresh_tokens`
- `journal_entries`
- `community_feed_snapshot`
- `community_media`

说明：

- 继续使用 Mongo 不影响当前联调；
- 但若以长期中国区运营为目标，建议把“主业务查询与报表”逐步迁到 MySQL。

---

## 4. 从 Mongo 平滑迁移到 MySQL（不改 iOS 接口）

## 阶段 1：双模型准备

- 保持 API 契约不变（`/api/v1/...` 不改）
- 在 Repository 层增加 MySQL 实现
- 先写新表与索引，不切流

## 阶段 2：双写

- 写请求同时写 Mongo + MySQL
- 读请求仍以 Mongo 为主
- 增加对账任务（按 userId 校验条数与 hash）

## 阶段 3：读切换

- 灰度将读流量切到 MySQL（按用户分桶）
- 保留 Mongo 回退开关
- 监控查询延迟、错误率、数据一致性

## 阶段 4：收尾

- 停止 Mongo 双写，改为只读备份一段时间
- 完成数据冻结与归档策略
- 最终下线 Mongo 主链路职责

---

## 5. 你需要提供给我的数据库信息

## 5.1 当前阶段（Mongo 过渡）

- `MONGODB_URI`
- `REDIS_URL`
- `S3_ENDPOINT`
- `S3_BUCKET`
- `S3_ACCESS_KEY`
- `S3_SECRET_KEY`
- `S3_REGION`
- `JWT_SECRET`（32 位以上）

## 5.2 迁移阶段（切 MySQL）

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_SSL_MODE`（建议生产开启）

---

## 6. 最小上线建议（数据库维度）

- 第 1 批（必须）：
  - 主链路：登录、refresh、journal、社区发帖、媒体上传
  - Redis：验证码 + 登录限流
  - 对象存储：图片上传/访问
- 第 2 批（增强）：
  - AI 对话消息与 insights 入库
  - 审核与举报闭环
  - 迁移到 MySQL 双写与灰度读切换
