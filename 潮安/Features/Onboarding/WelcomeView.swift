import SwiftUI

struct WelcomeView: View {
    @EnvironmentObject var vm: AppViewModel

    var body: some View {
        VStack(spacing: 18) {
            IOSStatusBar()
            Spacer()

            RoundedRectangle(cornerRadius: 24)
                .fill(.white.opacity(0.88))
                .frame(width: 86, height: 86)
                .overlay(Image(systemName: "sparkles").font(.system(size: 34)).foregroundStyle(CATheme.primaryAlt))
                .shadow(color: .black.opacity(0.08), radius: 16, x: 0, y: 8)

            Text("潮安").font(.system(size: 42, weight: .bold)).foregroundStyle(CATheme.text)
            Text("继续前，请先登录或注册")
                .caText(.body)
                .foregroundStyle(CATheme.subText)

            FrostedCard {
                VStack(spacing: 10) {
                    Text("创建账号以保存你的记录与报告").font(.system(size: 14, weight: .semibold)).foregroundStyle(CATheme.subText)
                    authButton("使用手机号注册", icon: "iphone")
                    authButton("使用微信号注册", icon: "message")
                    authButton("已有账号登录", icon: "person")
                    Button("登录后进入潮安") { vm.login() }.buttonStyle(PrimaryButtonStyle())
                }
            }
            .padding(.horizontal, 20)

            Text("潮安提供的是健康支持与自我管理工具，不替代医生诊疗。若有严重不适，请及时就医。")
                .caText(.footnote)
                .foregroundStyle(CATheme.subText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)
            Spacer()
        }
    }

    @ViewBuilder
    private func authButton(_ title: String, icon: String) -> some View {
        Button {
            vm.login()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: icon)
                Text(title)
            }
            .font(.system(size: 14, weight: .semibold))
            .caText(.body)
            .foregroundStyle(CATheme.subText)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Capsule().fill(.white.opacity(0.76)))
            .overlay(Capsule().stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
