import SwiftUI

struct UserStatusView: View {
    @State private var selectedIndex: Int?
    private let options: [(icon: String, title: String, subtitle: String)] = [
        ("questionmark.circle", "我不确定自己是不是进入围绝经期", "最近有一些变化，想了解是否相关"),
        ("flame", "我已经有了一些明显症状", "潮热、失眠、情绪波动等"),
        ("moon", "我已经绝经", "关注绝经后的健康管理"),
        ("lightbulb", "我只是想提前了解", "为未来做准备")
    ]

    var body: some View {
        PageScaffold(title: "选择状态", showBack: true) {
            VStack(alignment: .leading, spacing: 12) {
                ProgressView(value: 0.25).tint(CATheme.primaryAlt)

                Text("你现在更接近哪种状态？")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(CATheme.text)
                Text("选择最接近的选项，我们可以为你提供更合适的内容")
                    .font(.system(size: 13))
                    .foregroundStyle(CATheme.subText)

                ForEach(options.indices, id: \.self) { idx in
                    let item = options[idx]
                    let selected = selectedIndex == idx
                    Button {
                        selectedIndex = idx
                    } label: {
                        HStack(spacing: 10) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(CATheme.lilac.opacity(0.7))
                                .frame(width: 46, height: 46)
                                .overlay(Image(systemName: item.icon).foregroundStyle(CATheme.primaryAlt))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(CATheme.text)
                                Text(item.subtitle)
                                    .font(.system(size: 12))
                                    .foregroundStyle(CATheme.subText)
                            }
                            Spacer()
                            Circle()
                                .stroke(selected ? CATheme.primaryAlt : CATheme.border, lineWidth: 2)
                                .frame(width: 22, height: 22)
                                .overlay(Circle().fill(selected ? CATheme.primaryAlt : .clear).frame(width: 12, height: 12))
                        }
                        .padding(12)
                        .background(RoundedRectangle(cornerRadius: 18).fill(selected ? CATheme.lilac.opacity(0.45) : .white.opacity(0.72)))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(selected ? CATheme.primaryAlt.opacity(0.45) : CATheme.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}
