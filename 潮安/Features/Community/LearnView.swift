import SwiftUI

struct LearnView: View {
    @EnvironmentObject var router: AppRouter
    @State private var selected = 0
    @State private var search = ""

    var body: some View {
        PageScaffold(title: "社区") {
            VStack(spacing: 12) {
                HStack {
                    Picker("", selection: $selected) {
                        Text("官方").tag(0)
                        Text("用户").tag(1)
                    }
                    .pickerStyle(.segmented)
                    Button {
                        router.push(.communityPost)
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: 16, weight: .bold))
                            .frame(width: 34, height: 34)
                            .background(Circle().fill(CATheme.primary.opacity(0.55)))
                    }
                    .buttonStyle(.plain)
                }
                TextField("搜索话题或关键词", text: $search)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 20).fill(.white.opacity(0.74)))

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                    ForEach(mockCommunityCards(selected: selected), id: \.title) { card in
                        Button {
                            router.push(card.isOfficial ? .articleDetail : .communityPostDetail)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(
                                        LinearGradient(
                                            colors: [CATheme.lilac.opacity(0.8), CATheme.blue.opacity(0.45)],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(height: card.height)
                                    .overlay(
                                        Image(systemName: card.isOfficial ? "book.closed" : "person.2")
                                            .font(.system(size: 28))
                                            .foregroundStyle(.white.opacity(0.85))
                                    )
                                Text(card.title)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(CATheme.text)
                                    .multilineTextAlignment(.leading)
                                Text(card.tag)
                                    .font(.system(size: 11))
                                    .foregroundStyle(CATheme.subText)
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: 20).fill(.white.opacity(0.62)))
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(CATheme.border, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func mockCommunityCards(selected: Int) -> [(title: String, tag: String, height: CGFloat, isOfficial: Bool)] {
        let official: [(title: String, tag: String, height: CGFloat, isOfficial: Bool)] = [
            ("更年期睡眠管理：四个晚间习惯", "官方科普", 130.0, true),
            ("如何识别潮热触发因素", "官方科普", 170.0, true),
            ("情绪波动时的呼吸调节法", "官方科普", 145.0, true),
            ("就医沟通前该准备什么", "官方科普", 160.0, true)
        ]
        let user: [(title: String, tag: String, height: CGFloat, isOfficial: Bool)] = [
            ("我把下午咖啡换成温水后的变化", "用户分享", 150.0, false),
            ("三周呼吸练习打卡小结", "用户分享", 190.0, false),
            ("今天又潮热了，但我没那么慌了", "用户分享", 135.0, false),
            ("和家人沟通的一点经验", "用户分享", 165.0, false)
        ]
        return selected == 0 ? official : user
    }
}
