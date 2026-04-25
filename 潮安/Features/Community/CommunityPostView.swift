import SwiftUI

struct CommunityPostView: View {
    @EnvironmentObject var router: AppRouter
    @State private var title = ""
    @State private var bodyText = ""
    @State private var selectedTags: Set<String> = []
    private let tags = ["话题", "提及用户", "投票", "潮热", "睡眠", "情绪"]

    var body: some View {
        PageScaffold(title: "发布分享", showBack: true) {
            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(CATheme.lilac.opacity(0.7))
                        .frame(width: 92, height: 92)
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(CATheme.border, style: StrokeStyle(lineWidth: 1, dash: [5]))
                        .frame(width: 92, height: 92)
                        .overlay(Image(systemName: "plus").font(.system(size: 24)).foregroundStyle(CATheme.subText.opacity(0.7)))
                    Spacer()
                }

                TextField("添加标题", text: $title)
                    .font(.system(size: 28, weight: .bold))
                    .padding(.vertical, 8)
                TextField("写下你的体验...", text: $bodyText, axis: .vertical)
                    .lineLimit(3...8)
                    .padding(.bottom, 12)
                    .overlay(Rectangle().fill(CATheme.border).frame(height: 1), alignment: .bottom)

                FlowTags(tags: tags, selected: $selectedTags)

                VStack(spacing: 8) {
                    settingLine("添加位置")
                    settingLine("公开可见")
                    settingLine("添加组件")
                    settingLine("高级设置")
                }

                Text("提醒：请基于个人体验分享，避免将个体体验表述为医疗结论。")
                    .font(.system(size: 12))
                    .foregroundStyle(CATheme.subText)

                HStack(spacing: 12) {
                    Button("保存草稿") {}
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(CATheme.text)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(RoundedRectangle(cornerRadius: 24).stroke(CATheme.border, lineWidth: 1))

                    Button("发布") {
                        router.resetToTab(.learn)
                    }
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(RoundedRectangle(cornerRadius: 24).fill(CATheme.primaryAlt))
                }
                .padding(.top, 2)
            }
        }
    }

    @ViewBuilder
    private func settingLine(_ title: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundStyle(CATheme.subText)
        }
        .font(.system(size: 14))
        .foregroundStyle(CATheme.text)
        .padding(.vertical, 10)
        .overlay(Rectangle().fill(CATheme.border.opacity(0.7)).frame(height: 1), alignment: .bottom)
    }
}
