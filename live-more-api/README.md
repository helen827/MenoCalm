# live-more-api

Spring Boot 3 后端，已收口为 **MySQL + Redis + S3 兼容对象存储** 单主链路（不再依赖 MongoDB / 双写灰度）。

## API（与客户端一致）

| 方法 | 路径 | 鉴权 |
|------|------|------|
| POST | `/api/v1/auth/phone/code/send` | 无 |
| POST | `/api/v1/auth/phone/code/verify` | 无 |
| POST | `/api/v1/auth/phone/login` | 无 |
| POST | `/api/v1/auth/wechat/login` | 无 |
| POST | `/api/v1/auth/test-account/login` | 无（需 `X-Test-Account-Secret`） |
| POST | `/api/v1/auth/refresh` | 无 |
| GET | `/api/v1/journal/entries?userId=` | Bearer JWT |
| PUT | `/api/v1/journal/entries?userId=` | Bearer JWT |
| GET | `/community/feed` | 无 |
| POST | `/api/v1/community/media/presign` | Bearer JWT |
| POST | `/api/v1/community/posts?userId=` | Bearer JWT |
| GET | `/api/v1/community/posts?userId=&limit=` | Bearer JWT |
| GET | `/api/v1/community/posts/page?userId=&cursor=&limit=` | Bearer JWT |
| GET | `/api/v1/community/posts/{postId}?userId=` | Bearer JWT |
| POST | `/api/v1/community/posts/{postId}/comments?userId=` | Bearer JWT |
| GET | `/api/v1/community/posts/{postId}/comments?userId=&limit=` | Bearer JWT |
| POST | `/api/v1/community/posts/{postId}/like?userId=` | Bearer JWT |
| DELETE | `/api/v1/community/posts/{postId}/like?userId=` | Bearer JWT |
| POST | `/api/v1/community/posts/{postId}/favorite?userId=` | Bearer JWT |
| DELETE | `/api/v1/community/posts/{postId}/favorite?userId=` | Bearer JWT |
| DELETE | `/api/v1/community/posts/{postId}?userId=` | Bearer JWT |
| DELETE | `/api/v1/community/posts/{postId}/comments/{commentId}?userId=` | Bearer JWT |
| GET | `/api/v1/conversations/messages?userId=&conversationId=&limit=` | Bearer JWT |
| POST | `/api/v1/conversations/messages?userId=` | Bearer JWT |
| GET | `/api/v1/conversations/insight?userId=&conversationId=` | Bearer JWT |
| PUT | `/api/v1/conversations/insight?userId=&conversationId=` | Bearer JWT |
| POST | `/api/v1/conversations/insight/{conversationId}/analyze?userId=` | Bearer JWT |
| POST | `/api/v1/conversations/{conversationId}/chat-reply?userId=` | Bearer JWT |
| POST | `/api/v1/analytics/weekly-report?userId=` | Bearer JWT |
| GET | `/api/v1/knowledge/docs?limit=` | 无 |
| GET | `/api/v1/knowledge/docs/{id}` | 无 |
| POST | `/api/v1/knowledge/docs` | Bearer JWT |
| DELETE | `/api/v1/knowledge/docs/{id}` | Bearer JWT |

## 本地运行

前置：JDK 17+、Maven 3.9+、Docker（用于 MySQL/Redis/MinIO）。

```bash
docker compose up -d
export SPRING_PROFILES_ACTIVE=dev
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/live_more?useSSL=false&characterEncoding=utf8'
export MYSQL_USER='root'
export MYSQL_PASSWORD='root'
mvn spring-boot:run
```

## 生产部署（阿里云）

无法在替你登录阿里云账号的前提下完成「一键代建」RDS/ECS 等操作。仓库已提供 **`Dockerfile`**（多阶段构建，Java 17）与 **[阿里云 ECS + 托管数据部署说明](docs/deploy-aliyun-ecs.md)**：含 RDS / Redis / OSS / ACR / `docker run` 环境变量示例及与管理站联调要点。

## 数据库初始化

```bash
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-core-schema.sql
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-journal-schema.sql
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-migration-symptoms-analytics.sql
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-migration-community-moderation-audit.sql
```

若使用 `docker compose` 中的 MinIO：启动后需在控制台 `http://127.0.0.1:9001` 创建与 `S3_BUCKET` 同名的桶（或使用 `mc mb`），并保证 `S3_*` 与 compose 中的 `MINIO_ROOT_*` 一致。

## 关键配置（环境变量）

| 变量 | 说明 |
|------|------|
| `MYSQL_URL` | MySQL JDBC URL |
| `MYSQL_USER` | MySQL 用户名 |
| `MYSQL_PASSWORD` | MySQL 密码 |
| `REDIS_URL` | Redis 连接串 |
| `JWT_SECRET` | HS256 密钥，至少 32 字符 |
| `JWT_ACCESS_SECONDS` | Access token TTL（秒） |
| `REFRESH_TOKEN_TTL_SECONDS` | Refresh token TTL（秒） |
| `LEGACY_PHONE_LOGIN_ENABLED` | 是否启用旧手机号直登接口（默认 `false`） |
| `TEST_ACCOUNT_LOGIN_ENABLED` | 是否启用测试账号登录接口（默认 `false`） |
| `TEST_ACCOUNT_SECRET` | 测试账号登录密钥（仅在启用测试登录时生效） |
| `SMS_PROVIDER` | 短信通道提供方（当前支持 `aliyun`） |
| `SMS_SIGN_NAME` | 阿里云短信签名 |
| `SMS_TEMPLATE_CODE` | 阿里云短信模板编码（模板变量需含 `code`） |
| `SMS_ACCESS_KEY_ID` | 阿里云 AccessKey ID |
| `SMS_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret |
| `SMS_REGION_ID` | 短信 Region，默认 `cn-hangzhou` |
| `SMS_CODE_TTL_SECONDS` | 验证码有效期（秒） |
| `SMS_RESEND_COOLDOWN_SECONDS` | 验证码重发冷却（秒） |
| `SMS_PER_PHONE_PER_HOUR_MAX` | 单手机号每小时发送上限 |
| `SMS_PER_IP_PER_HOUR_MAX` | 单 IP 每小时发送上限 |
| `SMS_MAX_VERIFY_ATTEMPTS` | 验证码最大校验失败次数 |
| `WECHAT_APP_ID` | 微信开放平台 AppID |
| `WECHAT_APP_SECRET` | 微信开放平台 AppSecret |
| `WECHAT_UNIVERSAL_LINK` | iOS 微信回调 Universal Link |
| `WECHAT_REDIRECT_URI` | iOS 微信回调 URI |
| `AI_PROVIDER` | AI 提供方（当前支持 `qiniu`） |
| `AI_ENDPOINT` | 七牛云大模型 OpenAI 兼容接口地址 |
| `AI_API_KEY` | 七牛云大模型 API Key |
| `AI_MODEL` | 七牛云模型名称 |
| `AI_CONNECT_TIMEOUT_MS` | AI 连接超时（毫秒） |
| `AI_READ_TIMEOUT_MS` | AI 响应超时（毫秒） |
| `AI_CHAT_TEMPERATURE` | 聊天补全温度（默认见 `application.yml`） |
| `AI_EXTRACTION_TEMPERATURE` | 症状抽取温度 |
| `AI_REPORT_TEMPERATURE` | 周报生成温度 |
| `CONVERSATIONS_DAILY_CONTEXT_ZONE_ID` | 同日对话上下文时区（`ZoneId`，如 `Asia/Shanghai`） |
| `COMMUNITY_SEED_ON_EMPTY` | 空库时是否导入 `community-feed.seed.json` |
| `S3_ENDPOINT` | S3 兼容端点 |
| `S3_BUCKET` | 对象存储桶 |
| `S3_ACCESS_KEY` | 对象存储 Access Key |
| `S3_SECRET_KEY` | 对象存储 Secret Key |
| `S3_REGION` | 对象存储 Region |
| `S3_PRESIGN_TTL_SECONDS` | 上传预签名有效期（秒） |
| `S3_PUBLIC_BASE_URL` | 公开访问域名（可选） |

## 测试

```bash
mvn test
```

## 联调最小验证

安全要求：所有受保护接口必须使用正式登录流程签发的 JWT（`/api/v1/auth/phone/code/verify` 或 `/api/v1/auth/wechat/login`）；不支持手工伪造 token 联调。
补充：在短信/微信未就绪阶段，可临时启用 `POST /api/v1/auth/test-account/login` 获取正式 JWT，但必须配置 `TEST_ACCOUNT_LOGIN_ENABLED=true` 且设置 `TEST_ACCOUNT_SECRET`，上线前应关闭该开关。

1. `POST /api/v1/auth/phone/code/send` 发送短信验证码
2. `POST /api/v1/auth/phone/code/verify` 用验证码换取 `accessToken/refreshToken`
3. `POST /api/v1/auth/wechat/login` 使用微信 `code` 注册/登录
4. `POST /api/v1/auth/refresh` 刷新令牌
5. `PUT /api/v1/journal/entries` 写入后再 `GET` 读回
6. `GET /community/feed` 获取社区列表
7. `POST /api/v1/community/media/presign` 获取上传预签名
8. `POST /api/v1/community/posts?userId=...` 发帖（支持 `tags`、`mediaIds`，会校验媒体归属）
9. `POST /api/v1/community/posts/{postId}/comments?userId=...` 发表评论
10. `POST /api/v1/community/posts/{postId}/like?userId=...` 点赞，`DELETE` 同路径取消点赞
11. `POST /api/v1/community/posts/{postId}/favorite?userId=...` 收藏，`DELETE` 同路径取消收藏
12. `DELETE /api/v1/community/posts/{postId}?userId=...` 删除自己的帖子（软删除）
13. `DELETE /api/v1/community/posts/{postId}/comments/{commentId}?userId=...` 删除自己的评论（软删除）
14. `GET /api/v1/community/posts/page?userId=...&limit=20` 首次分页，后续带上返回的 `nextCursor`
15. `POST /api/v1/conversations/messages?userId=...` 写入对话消息
16. `POST /api/v1/conversations/insight/{conversationId}/analyze?userId=...` 触发分析并落库（配置 `AI_PROVIDER=qiniu` 后调用七牛云）

微信开放平台参数准备见：`docs/wechat-open-platform-checklist.md`。

说明：`/api/v1/auth/phone/login` 为兼容历史客户端的旧接口，默认关闭；开启需设置 `LEGACY_PHONE_LOGIN_ENABLED=true`。
