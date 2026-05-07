# 微信开放平台参数清单（iOS App 登录）

## 必需参数

- `WECHAT_APP_ID`：开放平台移动应用的 AppID
- `WECHAT_APP_SECRET`：开放平台 AppSecret（仅后端保存）
- `WECHAT_UNIVERSAL_LINK`：iOS 微信登录回调 Universal Link
- `WECHAT_REDIRECT_URI`：iOS 备用回调 URI（如 `chaoan://wechat/callback`）

## 开放平台侧配置

1. 创建“移动应用”并通过资质审核。
2. 在应用配置里填写 iOS `Bundle ID`（需与 Xcode 一致）。
3. 配置并验证 `Universal Link` 关联域名。
4. 在微信登录能力中开通 `snsapi_userinfo` 相关权限。

## 本项目当前值（已从工程读取）

- `Team ID`：`RHG2TB2DDV`
- `Bundle ID`（主 App）：`com.jiaying.--`
- `Tests`：`com.jiaying.--Tests`（不用于微信申请）
- `UITests`：`com.jiaying.--UITests`（不用于微信申请）
- `Universal Link`：当前工程已配置 `applinks:proudmenopause.com`。

参考文档：
- `/Users/jiayinghe/Desktop/潮安/docs/WeChat-Universal-Link-Setup.md`

## 服务端回调交换流程

1. iOS 获取微信授权 `code`。
2. 调用 `POST /api/v1/auth/wechat/login`，请求体：`{"code":"..."}`。
3. 后端调用微信接口换取 `openid/unionid`，首登自动创建用户并返回业务 token。

## 数据库要求

- `users.phone_digits` 允许空（微信用户可不绑手机号）。
- 新增 `user_identities` 表保存第三方身份映射：
  - `provider`：固定 `wechat`
  - `provider_user_id`：优先 `unionid`，回退 `openid`
  - `provider_raw_id`：原始 `openid`
