# live-more-api

Spring Boot 3 后端，已收口为 **MySQL + Redis + S3 兼容对象存储** 单主链路（不再依赖 MongoDB / 双写灰度）。

## API（与客户端一致）

| 方法 | 路径 | 鉴权 |
|------|------|------|
| POST | `/api/v1/auth/phone/code/send` | 无 |
| POST | `/api/v1/auth/phone/code/verify` | 无 |
| POST | `/api/v1/auth/phone/login` | 无 |
| POST | `/api/v1/auth/wechat/login` | 无 |
| POST | `/api/v1/auth/refresh` | 无 |
| GET | `/api/v1/journal/entries?userId=` | Bearer JWT |
| PUT | `/api/v1/journal/entries?userId=` | Bearer JWT |
| GET | `/community/feed` | 无 |
| POST | `/api/v1/community/media/presign` | Bearer JWT |

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

## 数据库初始化

```bash
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-core-schema.sql
mysql -h127.0.0.1 -uroot -proot live_more < docs/mysql-journal-schema.sql
```

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

1. `POST /api/v1/auth/phone/code/send` 发送短信验证码
2. `POST /api/v1/auth/phone/code/verify` 用验证码换取 `accessToken/refreshToken`
3. `POST /api/v1/auth/wechat/login` 使用微信 `code` 注册/登录
4. `POST /api/v1/auth/refresh` 刷新令牌
5. `PUT /api/v1/journal/entries` 写入后再 `GET` 读回
6. `GET /community/feed` 获取社区列表
7. `POST /api/v1/community/media/presign` 获取上传预签名

微信开放平台参数准备见：`docs/wechat-open-platform-checklist.md`。
