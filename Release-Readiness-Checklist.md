# 潮安 Release Readiness Checklist（Week 4）

## 1) 灰度开关策略

- `cloudReadEnabled`：默认关闭；先内部账号白名单开启，再按用户分层放量。
- `cloudSyncEnabled`：默认关闭；在 remote 读取稳定后再开启写入。
- `failOpenToLocalData`：默认开启；远端异常时必须保障本地可用。
- 发布前确认：设置页可看到 rollout 与 sync 诊断面板，关键指标可视化可用。

## 2) 指标阈值基线

- 同步失败率（`syncFailureRate`）<= 15%
- 远端读取失败率（`remoteFailureRate`）<= 10%
- 医疗安全数据集召回率 >= 1.00（高危样本不得漏拦截）
- 医疗安全误报率 <= 0.34
- RAG 引用覆盖率 >= 0.67

## 3) 故障处理流程

1. 发现告警（设置页或 CI medical-eval 失败）。
2. 立即确认影响范围（仅云端？仅某用户？是否影响本地可用）。
3. 执行降级：关闭 `cloudSyncEnabled`，必要时关闭 `cloudReadEnabled`。
4. 保留现场：导出 xcresult、同步任务轨迹、失败错误码分布。
5. 修复后小流量回放：先恢复 cloudRead，再恢复 cloudSync。

## 4) 回滚演练脚本（建议每次发版前执行）

- 演练 A：注入远端 401 / 429 / 500，验证错误码映射与本地 fail-open。
- 演练 B：制造同步连续失败，验证 `SyncQueue` 生命周期轨迹和失败留痕。
- 演练 C：构造 local/remote revision 冲突，验证高 revision 获胜。
- 演练 D：运行 medical dataset gate，确认阈值达标。

## 5) 发布最终确认

- CI 四道门禁全绿：`build` / `unit-tests` / `ui-smoke` / `medical-eval`
- 无新增 blocker 级 lint/编译错误
- 关键文档已更新：`Architecture-Guide.md`、`Implementation-Checklist.md`、本检查单
