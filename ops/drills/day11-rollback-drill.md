# Day11 回滚演练记录

- 时间: 2026-04-25 23:46:51
- 触发条件: 模拟远端失败率连续超阈值
- 处置动作: 关闭 cloudSyncEnabled -> 关闭 cloudReadEnabled -> 保持 failOpenToLocalData=true
- 恢复动作: 核验指标后逐步恢复 cloudReadEnabled / cloudSyncEnabled
- 目标RTO: <= 15 分钟
- 结论: 回滚步骤可执行（待线上真实演练签字）