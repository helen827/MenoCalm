# 潮安 · 社区运营后台（Web）

独立的管理员前端，用于浏览全站帖子、下架帖子、删除评论。调用现有 `live-more-api` 的 `/api/v1/admin/community/**` 与社区评论接口。

## 前提条件

1. **后端** `live-more-api` 已启动，且 MySQL 中社区表可用。
2. 环境变量 `**COMMUNITY_ADMIN_USER_IDS`** 中写入 **当前登录用户的 JWT subject**（与测试登录返回的 `userID` 一致，例如 `phone_13800138000`）。多个用英文逗号分隔。
3. 本地开发时，Vite 将 `**/api` 代理**到 `http://127.0.0.1:8080`（可用环境变量 `VITE_DEV_API_PROXY` 覆盖）。

## 本地运行

```bash
cd live-more-admin-web
npm install
npm run dev
```

浏览器打开终端里提示的地址（一般为 `http://127.0.0.1:5173`）。

### 登录方式

- **测试账号登录**：填写手机号与 `TEST_ACCOUNT_SECRET`（请求头 `X-Test-Account-Secret` 同源逻辑）。需后端开启 `TEST_ACCOUNT_LOGIN_ENABLED`。
- **粘贴 Token**：将 App 或其它客户端拿到的 **access token** 粘贴进去，并填写对应的 **用户 ID**（用于拉取评论列表的 `userId` 查询参数）。

## 生产部署

1. 构建静态资源：
  ```bash
   npm run build
  ```
2. 将 `dist/` 目录部署到任意静态站点（Nginx、OSS + CDN 等）。
3. 构建时指定 API 根地址（**不要**以 `/` 结尾），例如：
  ```bash
   VITE_API_BASE=https://api.your-domain.com npm run build
  ```
4. **跨域**：若管理站与 API 不同源，请在 `live-more-api` 的 `CORS_ALLOWED_ORIGINS` 中加入管理页来源（例如 `https://admin.your-domain.com`）。

## 部署到 Render

仓库根目录已提供 `[render.yaml](../render.yaml)`，用于 **Render Blueprint**（静态站点 + SPA 回源 + 构建时注入 API 地址）。

### 你需要完成的步骤（我无法代你登录 Render）

1. 将当前改动 **push 到 GitHub / GitLab**（Render 从 Git 拉代码构建）。
2. 打开 [Render Dashboard](https://dashboard.render.com/) → **New** → **Blueprint**（或 **Static Site** 后手动对齐下面配置）。
3. 选中本仓库与分支，应用 Blueprint。首次创建时 Render 会要求填写 `**VITE_API_BASE`**：填你的 `**live-more-api` 公网根地址**（示例 `https://live-more-api-xxxx.onrender.com`，**不要**末尾 `/`）。
4. 部署成功后，记下静态站 URL（如 `https://chaoan-community-admin.onrender.com`）。
5. 在 `**live-more-api`** 的环境变量里把 `**CORS_ALLOWED_ORIGINS`** 设为包含该静态站 URL（多个来源用英文逗号分隔）。否则浏览器会拦截跨域请求。
6. 确认 `**COMMUNITY_ADMIN_USER_IDS`** 已包含你用于登录后台的账号 id。

### 与手动创建「Static Site」等价配置


| 项                 | 值                                     |
| ----------------- | ------------------------------------- |
| Root Directory    | `live-more-admin-web`                 |
| Build Command     | `npm install && npm run build`        |
| Publish directory | `dist`                                |
| 环境变量              | `VITE_API_BASE` = 你的 API 根 URL（构建时注入） |


本地开发仍用 `npm run dev`（Vite 代理 `/api`）；生产构建只认 `VITE_API_BASE`。

## 与后端的接口


| 功能   | 方法     | 路径                                                                                 |
| ---- | ------ | ---------------------------------------------------------------------------------- |
| 分页列表 | GET    | `/api/v1/admin/community/posts/page`                                               |
| 下架帖子 | DELETE | `/api/v1/admin/community/posts/{postId}`                                           |
| 删评论  | DELETE | `/api/v1/admin/community/posts/{postId}/comments/{commentId}`                      |
| 拉评论  | GET    | `/api/v1/community/posts/{postId}/comments?userId=…`（需 Bearer，且 `userId` 与 JWT 一致） |


所有请求均带 `Authorization: Bearer <accessToken>`；401 时会尝试用 `refreshToken` 调用 `/api/v1/auth/refresh`（测试登录已写入 sessionStorage）。