import SwiftUI

struct BasicInfoView: View {
    @State private var ageRange = ""
    @State private var periodDate = ""
    @State private var selectedRegular = 0
    @State private var isMenopause = false
    @State private var inTreatment = false
    @State private var reminder = true

    var body: some View {
        PageScaffold(title: "基础信息", showBack: true) {
            VStack(alignment: .leading, spacing: 12) {
                ProgressView(value: 0.5).tint(CATheme.primaryAlt)
                Text("这些信息只用于帮助你获得更合适的内容建议，你可以随时修改。")
                    .font(.system(size: 13))
                    .foregroundStyle(CATheme.subText)

                TextField("年龄段（如 45-50 岁）", text: $ageRange)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(.white.opacity(0.74)))
                TextField("最近一次月经时间（示例：2026-04-01）", text: $periodDate)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(.white.opacity(0.74)))

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("月经是否规律")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(CATheme.text)
                        Picker("", selection: $selectedRegular) {
                            Text("规律").tag(0)
                            Text("不规律").tag(1)
                        }
                        .pickerStyle(.segmented)
                    }
                }

                FrostedCard {
                    VStack(spacing: 8) {
                        infoToggle("是否已绝经", "选择后将调整推荐内容", isOn: $isMenopause)
                        infoToggle("是否正在接受相关治疗", "包括 HRT、中药等", isOn: $inTreatment)
                        infoToggle("开启记录提醒", "帮助你养成记录习惯", isOn: $reminder)
                    }
                }

                Button("继续") {}
                    .buttonStyle(PrimaryButtonStyle())
            }
        }
    }

    @ViewBuilder
    private func infoToggle(_ title: String, _ subtitle: String, isOn: Binding<Bool>) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundStyle(CATheme.text)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(CATheme.subText)
            }
            Spacer()
            Toggle("", isOn: isOn).labelsHidden().tint(CATheme.primaryAlt)
        }
        .font(.system(size: 14))
    }
}
