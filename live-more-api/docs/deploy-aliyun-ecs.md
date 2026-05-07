# 在阿里云部署 live-more-api（ECS + 托管数据）

本文说明一种常见、可控的组合：**容器镜像部署在 ECS**，数据库与缓存使用阿里云托管产品（RDS、Redis），对象存储使用 **OSS（S3 兼容访问）**。我无法替你登录阿里云或创建付费资源；按下列步骤在你自己的账号中完成即可。

## 1. 需要准备的阿里云资源

| 资源 | 用途 |
|------|------|
| **VPC + 交换机** | ECS、RDS、Redis 建议同一 VPC，内网互通 |
| **RDS MySQL 8.x** | 业务库；创建数据库名例如 `live_more`，账号与 `MYSQL_*` 一致 |
| **云数据库 Redis 版** | 对应 `REDIS_URL`（控制台可拷贝带密码的连接串） |
| **对象存储 OSS** | 对应 `S3_*`；创建 Bucket，使用 **RAM 子账号** AccessKey（最小权限） |
| **容器镜像服务 ACR** | 存放 `live-more-api` 镜像（个人版实例即可起步） |
| **ECS（推荐 Alibaba Cloud Linux / Ubuntu）** | 安装 Docker，拉取 ACR 镜像并运行 |
| **（推荐）SLB + 证书** 或 **ECS 前 Nginx** | 对外提供 **HTTPS**；大陆域名通常需 **ICP 备案** |

短信、微信、七牛 AI 等仍按 `.env.example` 与实际业务配置。

## 2. 初始化 MySQL

在 RDS 上创建库 `live_more` 后，按顺序执行（连接串与账号替换为你的 RDS）：

```bash
mysql -h rm-xxxx.mysql.rds.aliyuncs.com -uYOUR_USER -p live_more < docs/mysql-core-schema.sql
mysql -h rm-xxxx.mysql.rds.aliyuncs.com -uYOUR_USER -p live_more < docs/mysql-journal-schema.sql
mysql -h rm-xxxx.mysql.rds.aliyuncs.com -uYOUR_USER -p live_more < docs/mysql-migration-symptoms-analytics.sql
mysql -h rm-xxxx.mysql.rds.aliyuncs.com -uYOUR_USER -p live_more < docs/mysql-migration-community-moderation-audit.sql
```

若某迁移脚本在你当前库版本已执行过，可跳过（以 DBA 策略为准）。

## 3. OSS 与 `S3_*` 环境变量

本服务使用 AWS SDK 的 S3 客户端，并已开启 **path-style**（见 `S3StorageConfig`），可与 OSS **兼容模式** 对接。

典型配置示例（华东 1，请按控制台实际地域修改）：

- `S3_ENDPOINT`：`https://oss-cn-hangzhou.aliyuncs.com`
- `S3_REGION`：`cn-hangzhou`（与 SDK `Region` 一致）
- `S3_BUCKET`：你的 Bucket 名
- `S3_ACCESS_KEY` / `S3_SECRET_KEY`：RAM 用户密钥
- `S3_PUBLIC_BASE_URL`：若对公网读公开对象，可填 OSS 静态域名或 CDN 域名（与 `CommunityMediaPublicUrlBuilder` 逻辑一致；不需要可留空按代码默认）

在 OSS 控制台为 Bucket 配置跨域（CORS），允许 App / 管理站来源与你的上传域名，按需允许 `PUT`/`GET`/`HEAD`。

## 4. 构建镜像并推送到 ACR

在开发机（已安装 Docker）的 **`live-more-api` 目录**执行：

```bash
docker build -t live-more-api:0.1.0 .
```

登录 ACR（地域、命名空间、仓库名替换为你的；以下为示例域名）：

```bash
docker login --username=你的阿里云账号 registry.cn-hangzhou.aliyuncs.com
docker tag live-more-api:0.1.0 registry.cn-hangzhou.aliyuncs.com/你的命名空间/live-more-api:0.1.0
docker push registry.cn-hangzhou.aliyuncs.com/你的命名空间/live-more-api:0.1.0
```

## 5. 在 ECS 上运行

安全组放行 **8080**（或仅内网 + SLB 健康检查端口）。示例：

```bash
docker pull registry.cn-hangzhou.aliyuncs.com/你的命名空间/live-more-api:0.1.0
docker run -d --name live-more-api --restart unless-stopped -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_URL='jdbc:mysql://rm-xxxx.mysql.rds.aliyuncs.com:3306/live_more?useSSL=true&characterEncoding=utf8' \
  -e MYSQL_USER='...' \
  -e MYSQL_PASSWORD='...' \
  -e REDIS_URL='redis://:密码@r-xxxx.redis.rds.aliyuncs.com:6379' \
  -e JWT_SECRET='至少32字符的随机串' \
  -e S3_ENDPOINT='https://oss-cn-hangzhou.aliyuncs.com' \
  -e S3_REGION='cn-hangzhou' \
  -e S3_BUCKET='你的bucket' \
  -e S3_ACCESS_KEY='...' \
  -e S3_SECRET_KEY='...' \
  -e CORS_ALLOWED_ORIGINS='https://chaoan-community-admin.onrender.com' \
  registry.cn-hangzhou.aliyuncs.com/你的命名空间/live-more-api:0.1.0
```

生产环境请把其余变量（短信、微信、AI、`COMMUNITY_ADMIN_USER_IDS` 等）按 `.env.example` 与运维规范补全；敏感信息用 **阿里云 KMS / 环境注入**，不要写进镜像。

验证：

```bash
curl -sS "http://ECS公网或SLB:8080/actuator/health"
```

对外仅暴露 **HTTPS** 时，在 Nginx/SLB 上反代到容器 `8080`，并把 App、Render 管理站的 **`VITE_API_BASE` / `?api=`** 设为 **`https://你的 API 域名`**（无尾 `/`）。

## 6. 与管理后台、App 联调

- **Render 静态管理站**：在对应服务的 Environment 中设置 `VITE_API_BASE=https://你的API域名` 并重新部署；或在浏览器使用 `?api=https://你的API域名` 一次性写入本地。
- **`CORS_ALLOWED_ORIGINS`**：包含 `https://chaoan-community-admin.onrender.com` 以及 iOS / Web 实际来源。
- **`COMMUNITY_ADMIN_USER_IDS`**：包含管理员 JWT `sub`。

## 7. 其他部署形态

- **SAE / ACK**：同样使用本 `Dockerfile` 构建的镜像，把上述环境变量配置到编排模板中即可。
- **仅内网 RDS/Redis**：`MYSQL_URL`、`REDIS_URL` 使用 VPC 内网地址，ECS 与数据库同 VPC。

若你希望后续增加 **GitHub Actions 自动构建并推送 ACR**，可在有 `ACR_USERNAME` / `ACR_PASSWORD` 等 Secrets 的前提下再加一条流水线（需单独维护密钥）。
