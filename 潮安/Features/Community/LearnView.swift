import SwiftUI

struct LearnView: View {
    @EnvironmentObject var router: AppRouter
    @EnvironmentObject var vm: AppViewModel
    @State private var selected = 0
    @State private var search = ""

    private var displayedCards: [CommunityFeedItem] {
        let base = selected == 0 ? vm.communityFeedOfficial : vm.communityFeedUser
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return base }
        return base.filter {
            $0.title.localizedCaseInsensitiveContains(q) || $0.tag.localizedCaseInsensitiveContains(q)
        }
    }

    var body: some View {
        PageScaffold(title: "社区") {
            VStack(spacing: 12) {
                if !vm.isNetworkReachable, vm.communityFeedErrorMessage == nil {
                    FrostedCard {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "wifi.slash")
                                .foregroundStyle(.orange)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("当前无网络，远程社区内容可能无法更新；可继续浏览已加载内容或稍后下拉重试。")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundStyle(CATheme.text)
                            }
                            Spacer(minLength: 0)
                        }
                    }
                }

                if let error = vm.communityFeedErrorMessage {
                    FrostedCard {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundStyle(.orange)
                            VStack(alignment: .leading, spacing: 6) {
                                Text(error)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundStyle(CATheme.text)
                                Button("重试加载") {
                                    Task { await vm.refreshCommunityFeed() }
                                }
                                .buttonStyle(.plain)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(CATheme.primaryAlt)
                            }
                            Spacer(minLength: 0)
                        }
                    }
                }

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

                if vm.isCommunityFeedLoading && displayedCards.isEmpty {
                    FrostedCard {
                        HStack(spacing: 10) {
                            ProgressView()
                            Text("社区内容加载中...")
                                .font(.system(size: 13))
                                .foregroundStyle(CATheme.subText)
                            Spacer(minLength: 0)
                        }
                    }
                } else if displayedCards.isEmpty {
                    Text(selected == 0 ? "暂无官方内容，请下拉刷新或稍后再试。" : "暂无用户分享，欢迎发布你的经验（需符合社区规范）。")
                        .font(.system(size: 13))
                        .foregroundStyle(CATheme.subText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                    Button("点击重试") {
                        Task { await vm.refreshCommunityFeed() }
                    }
                    .buttonStyle(SecondaryButtonStyle())
                } else {
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                        ForEach(displayedCards) { card in
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
                                        .frame(height: card.heightPoints)
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
        .refreshable {
            await vm.refreshCommunityFeed()
        }
        .task {
            if vm.communityFeedOfficial.isEmpty, vm.communityFeedUser.isEmpty {
                await vm.refreshCommunityFeed()
            }
        }
    }
}
