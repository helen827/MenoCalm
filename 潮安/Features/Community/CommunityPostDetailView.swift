import SwiftUI

struct CommunityPostDetailView: View {
    @State private var reply = ""

    var body: some View {
        PageScaffold(title: "帖子详情", showBack: true) {
            VStack(spacing: 12) {
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Circle()
                                .fill(CATheme.lilac.opacity(0.7))
                                .frame(width: 34, height: 34)
                                .overlay(Image(systemName: "person.fill").foregroundStyle(CATheme.primaryAlt))
                            VStack(alignment: .leading, spacing: 2) {
                                Text("一颗金桔 JinJi")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("2025-05-12 · Guangdong")
                                    .font(.system(size: 11))
                                    .foregroundStyle(CATheme.subText)
                            }
                            Spacer()
                            Text("Follow")
                                .font(.system(size: 12, weight: .semibold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Capsule().stroke(CATheme.primaryAlt, lineWidth: 1))
                        }

                        RoundedRectangle(cornerRadius: 16)
                            .fill(
                                LinearGradient(
                                    colors: [CATheme.blue.opacity(0.55), CATheme.lilac.opacity(0.85)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(height: 200)

                        Text("斯坦福教授：40+ 女性 11 条饮食&运动建议")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(CATheme.text)
                        Text("更年期是每位女性生命中的必经之路。超过 2.8 亿中国女性正在经历这场蜕变。")
                            .font(.system(size: 14))
                            .foregroundStyle(CATheme.text)

                        HStack {
                            Label("347", systemImage: "heart")
                            Label("578", systemImage: "star")
                            Label("4", systemImage: "bubble.right")
                        }
                        .font(.system(size: 12))
                        .foregroundStyle(CATheme.subText)
                    }
                }
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("评论")
                            .font(.system(size: 14, weight: .bold))
                        commentRow("Diana", "感谢")
                        commentRow("一颗金桔 JinJi", "赶在母亲节的晚上，最近两周都在研究更年期。")
                    }
                    .font(.system(size: 13))
                    .foregroundStyle(CATheme.text)
                }
                HStack {
                    TextField("回复作者...", text: $reply)
                        .padding(12)
                        .background(RoundedRectangle(cornerRadius: 20).fill(.white.opacity(0.7)))
                    Button("发送") {}
                        .buttonStyle(PrimaryButtonStyle())
                        .frame(width: 82)
                }
            }
        }
    }

    @ViewBuilder
    private func commentRow(_ user: String, _ content: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(user)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(CATheme.subText)
            Text(content)
                .font(.system(size: 13))
                .foregroundStyle(CATheme.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white.opacity(0.7)))
    }
}
