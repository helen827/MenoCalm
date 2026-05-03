# App Store / 分发上架检查清单

与 [Release-Acceptance-Checklist](./Release-Acceptance-Checklist.md) 配合使用。

## 元数据

- [ ] 应用名称、副标题、关键词与品牌一致（MenoCalm / 潮安）
- [ ] 截图：AI 对话、呼吸练习、社区、报告/我的（共 5–10 张）
- [ ] 宣传文本与 PRD 定位一致：生活方式与科普支持，**非医疗诊断**

## 隐私与合规

- [ ] App Privacy 标签与 [Legal/Privacy-Policy.md](../Legal/Privacy-Policy.md) 一致
- [ ] 用户协议、隐私政策、医疗免责声明 URL 或应用内入口有效
- [ ] 首次使用已包含免责声明确认（见 Welcome 流程）

## 技术

- [ ] Release 构建：`resolve()` 生产配置正确，无调试后门
- [ ] 版本号 / Build 号递增
- [ ] 加密出口合规（若适用）问卷已填

## 发布后

- [ ] 首日监控：崩溃率、登录、同步、社区 Feed 错误
- [ ] 灰度与回滚预案可执行（见 ops 既有演练文档）
