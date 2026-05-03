# Deprecated Backend (Archive Only)

`backend/backend` 是临时初始化的最小 Spring Boot 脚手架，仅用于早期试跑 Maven 环境。

从当前阶段开始，仓库唯一后端主工程为：

- `live-more-api/`

请不要在本目录继续开发业务代码，避免与主后端并行演进导致接口和配置漂移。

## 处理建议

- 新功能开发：仅在 `live-more-api/` 进行
- 联调与测试：仅运行 `live-more-api/` 服务
- 本目录保留为归档参考，后续可在团队确认后删除
