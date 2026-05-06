# 微信 Universal Link 配置清单（可直接照做）

## 1) 你当前项目已定位到的值

- `Team ID`：`RHG2TB2DDV`
- `Bundle ID`（主 App）：`com.jiaying.--`
- `Tests Bundle ID`：`com.jiaying.--Tests`
- `UITests Bundle ID`：`com.jiaying.--UITests`

说明：微信开放平台一般填写主 App 的 `Bundle ID`，不是 `Tests/UITests`。

## 2) Xcode 已完成配置

- 已添加 entitlements：`潮安/潮安.entitlements`
- 已开启 Associated Domains：`applinks:proudmenopause.com`
- 已在 Debug/Release 加入 `CODE_SIGN_ENTITLEMENTS`

## 3) 服务器需部署 AASA 文件

- 模板文件：`docs/apple-app-site-association.template`
- 可直接发布文件：`docs/apple-app-site-association`
- 发布路径（两者任选其一，建议都放）：
  - `https://你的域名/.well-known/apple-app-site-association`
  - `https://你的域名/apple-app-site-association`
- 要求：
  - 不带 `.json` 后缀
  - `Content-Type` 建议 `application/json`
  - 可直接 HTTPS 访问，不能重定向到登录页

## 4) 微信开放平台申请表填写建议

- `Bundle ID`：`com.jiaying.--`（上线前改成正式包名）
- `测试版本 Bundle ID`：若无独立测试包，可与正式一致
- `Universal Links`：`https://你的域名/`（末尾保留 `/`）

## 5) 上线前必须替换

- 把 `com.jiaying.--` 改成正式 Bundle ID（例如 `com.xxx.chaoan`）
- 把 AASA 中的 `appID` 改成：`TeamID.正式BundleID`

当前 AASA 文件使用的是：
- `TeamID`：`RHG2TB2DDV`
- `BundleID`：`com.jiaying.--`
