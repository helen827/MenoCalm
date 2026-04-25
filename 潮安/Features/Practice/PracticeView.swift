import SwiftUI

struct PracticeView: View {
    @EnvironmentObject var router: AppRouter

    var body: some View {
        PageScaffold(title: "呼吸练习") {
            VStack(alignment: .leading, spacing: 12) {
                Text("在需要的时候，给你即时的放松")
                    .font(.system(size: 14))
                    .foregroundStyle(CATheme.subText)

                HStack(spacing: 6) {
                    Image(systemName: "arrow.left.and.right")
                    Text("左右滑动切换场景和声音")
                }
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0x765D67))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Capsule().fill(CATheme.lilac.opacity(0.55)))

                FrostedCard {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(.white.opacity(0.75))
                            .frame(width: 56, height: 56)
                            .overlay(Image(systemName: "clock").font(.system(size: 24)).foregroundStyle(CATheme.primaryAlt))
                        VStack(alignment: .leading, spacing: 4) {
                            Text("你最近记录了睡眠差")
                                .font(.system(size: 16, weight: .semibold))
                            Text("推荐今晚试试睡前身体扫描")
                                .font(.system(size: 13))
                                .foregroundStyle(CATheme.subText)
                        }
                    }
                }

                Text("按场景选择")
                    .font(.system(size: 16, weight: .bold))

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    practiceCard("潮热来临时", "90秒降温呼吸", "flame", "90秒")
                    practiceCard("夜间醒来后", "重新入睡练习", "moon", "5分钟")
                    practiceCard("睡前放松", "身体扫描", "sparkles", "5-8分钟")
                    practiceCard("焦虑心悸时", "慢呼吸练习", "heart", "3分钟")
                }

                Text("情绪与专注")
                    .font(.system(size: 16, weight: .bold))

                practiceRow("情绪暂停练习", "情绪快爆发时，给自己一个缓冲", "pause.circle")
                practiceRow("脑雾专注恢复", "注意力下降时，重新聚焦", "brain.head.profile")

                FrostedCard {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "info.circle")
                        Text("练习小贴士：建议在安静环境下练习，配合耳机效果更佳。如果某个练习不适合你，可以跳过尝试其他的。")
                            .font(.system(size: 12))
                            .foregroundStyle(CATheme.subText)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func practiceCard(_ title: String, _ subtitle: String, _ icon: String, _ duration: String) -> some View {
        Button {
            router.push(.practiceDetail)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: [CATheme.lilac.opacity(0.75), CATheme.primary.opacity(0.4)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 90)
                    .overlay(Image(systemName: icon).font(.system(size: 30)).foregroundStyle(.white.opacity(0.9)))
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(CATheme.text)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(CATheme.subText)
                HStack {
                    Text(duration)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(CATheme.subText)
                    Spacer()
                }
            }
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 22).fill(.white.opacity(0.62)))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func practiceRow(_ title: String, _ subtitle: String, _ icon: String) -> some View {
        Button {
            router.push(.practiceDetail)
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(LinearGradient(colors: [CATheme.primary.opacity(0.5), CATheme.primaryAlt.opacity(0.7)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 74, height: 74)
                    .overlay(Image(systemName: icon).font(.system(size: 30)).foregroundStyle(.white.opacity(0.9)))

                VStack(alignment: .leading, spacing: 5) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(CATheme.text)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(CATheme.subText)
                }
                Spacer()
            }
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 20).fill(.white.opacity(0.6)))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
