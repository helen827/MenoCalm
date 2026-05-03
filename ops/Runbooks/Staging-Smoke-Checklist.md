# Staging 冒烟与灰度开关检查单（Day7）

## 核心流程冒烟

- [ ] 首次启动 -> onboarding -> 登录成功
- [ ] AI 对话发送消息，记录可落库
- [ ] 趋势报告页可打开，数据渲染正常
- [ ] 设置页可查看上线监控、同步排障、运行告警
- [ ] 隐私政策/用户协议/医疗免责声明可访问

## 灰度开关验证

- [ ] `cloudReadEnabled = false` 时仅本地读取
- [ ] `cloudReadEnabled = true` 时可走远端读取
- [ ] `cloudSyncEnabled = false` 时不触发云端写入
- [ ] `cloudSyncEnabled = true` 时触发同步队列
- [ ] `failOpenToLocalData = true` 时远端失败可降级本地

## 验收结论

- [ ] 无阻塞缺陷（P0/P1）
- [ ] 可进入 Day8 合规收口阶段
