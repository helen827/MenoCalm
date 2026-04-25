import SwiftUI

struct SelfTestResultView: View {
    @EnvironmentObject var router: AppRouter

    var body: some View {
        PageScaffold(title: "自测结果", showBack: true) {
            VStack(spacing: 12) {
                FrostedCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("中度相关")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(.orange)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Capsule().fill(Color.orange.opacity(0.12)))

                        Text("你的近期表现与围绝经期常见变化有一定相关")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(CATheme.text)

                        HStack {
                            Text("相关程度")
                            Spacer()
                            Text("62%").foregroundStyle(CATheme.subText)
                        }
                        .font(.system(size: 13))

                        ProgressView(value: 0.62).tint(CATheme.primaryAlt)

                        HStack(spacing: 6) {
                            ForEach(["睡眠差", "脑雾", "心悸", "疲惫"], id: \.self) { chip in
                                Text(chip)
                                    .font(.system(size: 11))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(Capsule().fill(CATheme.lilac.opacity(0.78)))
                            }
                        }
                    }
                }

                FrostedCard {
                    Text("温馨提示：围绝经期是正常生理阶段。页面内容用于帮助观察和表达，不替代医生诊疗建议。")
                        .font(.system(size: 13))
                        .foregroundStyle(CATheme.subText)
                }

                Button("开始 AI 对话记录") {
                    router.resetToTab(.home)
                }
                .buttonStyle(PrimaryButtonStyle())
            }
        }
    }
}
