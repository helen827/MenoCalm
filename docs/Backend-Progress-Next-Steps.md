# 潮安后端进度检查与下一步实施清单

## 1. 结论先行

- `menocalm-api/` 才是当前应继续开发的主后端工程。
- `backend/backend` 是后续临时初始化的最小脚手架，不应作为主线。
- 建议后续所有后端开发、联调、CI 全部收敛到 `menocalm-api/`。

---

## 2. 当前真实进度（基于 menocalm-api）

### 2.1 工程与运行
- 技术栈：Spring Boot + Maven（`pom.xml` 已存在）
- 代码结构已具备产品化分层：`config/domain/repo/service/security/web`
- 已配置 MongoDB、JWT、CORS、Actuator health（`application.yml`）
- 已通过本地测试：`/Users/jiayinghe/tools/apache-maven-3.9.9/bin/mvn test`

### 2.2 已落地接口（与 iOS 契约对齐）
- 认证：
  - `POST /api/v1/auth/phone/login`
  - `POST /api/v1/auth/refresh`
- 日志：
  - `GET /api/v1/journal/entries?userId=...`
  - `PUT /api/v1/journal/entries?userId=...`
- 社区：
  - `GET /community/feed`
- 错误处理：
  - 已有全局异常处理 `RestExceptionHandler`

### 2.3 已有安全能力
- JWT 服务与过滤器已存在：`JwtService`、`JwtAuthenticationFilter`
- 安全配置已存在：`SecurityConfig`
- 启动时 JWT 密钥校验已存在：`JwtSecretStartupValidator`

---

## 3. 后端还需要做什么（按优先级）

## P0（立即）
- 明确“单后端目录”决策：主后端固定为 `menocalm-api/`
- 删除或归档 `backend/backend`（至少在文档中标记弃用，避免团队误用）
- 补齐接口契约文档（OpenAPI 或 README API 表格），对齐 iOS 字段命名和错误语义
- 增加最小集成测试覆盖：
  - `auth` 登录/刷新
  - `journal` 读写
  - JWT 鉴权失败场景

## P1（本周）
- 增加数据一致性保障：
  - `journal` 写入并发策略（版本号或最后写入策略明确）
  - `userId` 与 token 主体不一致时的错误码约定
- 增加运维基础：
  - 请求 traceId
  - 关键接口结构化日志
  - 健康检查增加依赖项（如 Mongo 可用性）

## P2（1-2 周）
- 将 `menocalm-api` 纳入仓库 CI（build + test + basic security gate）
- 增加接口契约回归（防止字段变更破坏 iOS）
- 增加限流与风控兜底（登录、刷新、写入接口）

---

## 4. 与 iOS 联调建议

- iOS 端运行时 `backend baseURL` 指向 `menocalm-api` 服务地址
- 先验证四条主链路：
  - 手机号登录
  - token 刷新
  - 获取日志列表
  - 上传日志覆盖
- 再验证社区回退链路：
  - `GET /community/feed` 成功
  - 服务失败时 iOS 本地回退提示正常

---

## 5. 今天可执行动作

- 在仓库根目录新增一条规则：后端主目录为 `menocalm-api/`
- 给 `menocalm-api/README.md` 增补“启动 + 环境变量 + API 列表 + 联调步骤”
- 增加 3-5 个集成测试用例，覆盖认证与日志核心路径

> 更新后的结论：后端并非“刚起步”，而是“已有完整骨架与核心接口”。下一阶段重点应从“新建后端”切换为“收敛目录、补测试、稳联调与上 CI”。
