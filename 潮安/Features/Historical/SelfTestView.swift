import SwiftUI

struct SelfTestView: View {
    @State private var selected: Int?
    private let choices = [
        "没有明显变化",
        "偶尔睡不好（每周 1-2 天）",
        "经常睡不好（每周 3-4 天）",
        "几乎每天都睡不好",
        "不确定"
    ]

    var body: some View {
        PageScaffold(title: "症状自测", showBack: true) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    ProgressView(value: 0.6).tint(CATheme.primaryAlt)
                    Text("3/5")
                        .font(.system(size: 12))
                        .foregroundStyle(CATheme.subText)
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Circle()
                                .fill(CATheme.primary.opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay(Image(systemName: "moon").foregroundStyle(CATheme.primaryAlt))
                            Text("睡眠变化")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(CATheme.primaryAlt)
                        }
                        Text("最近 3 个月，你的睡眠质量是否发生变化？")
                            .font(.system(size: 21, weight: .bold))
                        Text("包括入睡困难、夜间醒来、早醒等情况")
                            .font(.system(size: 13))
                            .foregroundStyle(CATheme.subText)
                    }
                }

                ForEach(choices.indices, id: \.self) { idx in
                    let isOn = selected == idx
                    Button {
                        selected = idx
                    } label: {
                        HStack {
                            Circle()
                                .stroke(isOn ? CATheme.primaryAlt : CATheme.border, lineWidth: 2)
                                .frame(width: 22, height: 22)
                                .overlay(Circle().fill(isOn ? CATheme.primaryAlt : .clear).frame(width: 12, height: 12))
                            Text(choices[idx])
                                .font(.system(size: 14))
                                .foregroundStyle(CATheme.text)
                            Spacer()
                        }
                        .padding(14)
                        .background(RoundedRectangle(cornerRadius: 14).fill(isOn ? CATheme.primary.opacity(0.2) : .white.opacity(0.74)))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(isOn ? CATheme.primaryAlt.opacity(0.5) : CATheme.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }

                FrostedCard {
                    Text("本测试仅供参考，不做医学诊断。如有严重不适，请及时就医。")
                        .font(.system(size: 12))
                        .foregroundStyle(CATheme.subText)
                }
            }
        }
    }
}
