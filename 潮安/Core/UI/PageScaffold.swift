import SwiftUI

struct PageScaffold<Content: View>: View {
    let title: String
    let showBack: Bool
    let onRefresh: (() async -> Void)?
    let content: Content

    init(title: String, showBack: Bool = false, onRefresh: (() async -> Void)? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.showBack = showBack
        self.onRefresh = onRefresh
        self.content = content()
    }

    @EnvironmentObject private var router: AppRouter

    var body: some View {
        VStack(spacing: CASpacing.sm) {
            HStack {
                if showBack {
                    Button {
                        router.pop()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(CATheme.text)
                            .frame(width: 34, height: 34)
                            .background(Circle().fill(.white.opacity(0.6)))
                    }
                }
                Text(title)
                    .caText(.pageTitle)
                    .foregroundStyle(CATheme.text)
                Spacer()
            }
            .padding(.horizontal, 18)

            Group {
                let scroll = ScrollView(showsIndicators: false) {
                    content
                        .padding(.horizontal, 18)
                        .padding(.bottom, CASpacing.lg)
                }
                if let onRefresh {
                    scroll.refreshable { await onRefresh() }
                } else {
                    scroll
                }
            }
        }
    }
}
