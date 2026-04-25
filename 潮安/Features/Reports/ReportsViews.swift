import SwiftUI

struct TrendReportView: View {
    @EnvironmentObject var vm: AppViewModel
    @EnvironmentObject var router: AppRouter
    @State private var selectedRange = 1
    private let ranges = [7, 30, 90]

    var body: some View {
        let snapshot = vm.reportSnapshot(rangeDays: ranges[selectedRange])

        PageScaffold(title: "趋势报告", showBack: true) {
            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    ForEach(["7天", "30天", "90天"].indices, id: \.self) { idx in
                        let active = idx == selectedRange
                        Button {
                            selectedRange = idx
                        } label: {
                            Text(["7天", "30天", "90天"][idx])
                                .font(.system(size: 14, weight: .medium))
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(Capsule().fill(active ? CATheme.primary : .white.opacity(0.72)))
                                .overlay(Capsule().stroke(active ? .clear : CATheme.border, lineWidth: 1))
                                .foregroundStyle(active ? .white : CATheme.subText)
                        }
                        .buttonStyle(.plain)
                    }
                    Spacer()
                }

                if snapshot.sampleCount == 0 {
                    FrostedCard {
                        Text("暂未发现历史记录。去 AI 对话页记录今天状态后，将在这里生成趋势说明。")
                            .font(.system(size: 14))
                            .foregroundStyle(CATheme.subText)
                    }
                } else {
                    FrostedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("近\(ranges[selectedRange])天概况")
                                .font(.system(size: 16, weight: .bold))
                            Text("记录条数：\(snapshot.sampleCount) 条。")
                            Text("高频症状：\(formatTopItems(snapshot.topSymptoms))")
                            Text("高频因素：\(formatTopItems(snapshot.topFactors))")
                        }
                        .font(.system(size: 13))
                        .foregroundStyle(CATheme.text)
                    }
                    FrostedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("症状-因素关联（Top）").font(.system(size: 14, weight: .bold))
                            if snapshot.correlations.isEmpty {
                                Text("暂未形成稳定关联，继续记录将提升分析准确性。")
                                    .font(.system(size: 13))
                                    .foregroundStyle(CATheme.subText)
                            } else {
                                ForEach(snapshot.correlations, id: \.id) { item in
                                    Text("\(item.symptom) ↔ \(item.factor)：\(item.count) 次")
                                        .font(.system(size: 13))
                                }
                            }
                        }
                    }
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("推荐下一步")
                            .font(.system(size: 14, weight: .bold))
                        Text(recommendationText(from: snapshot))
                            .font(.system(size: 13))
                        Text("提示：此分析用于日常观察，不构成医学诊断。")
                            .font(.system(size: 12))
                            .foregroundStyle(CATheme.subText)
                    }
                }

                Button("生成就医清单") {
                    router.push(.medicalList)
                }
                .buttonStyle(PrimaryButtonStyle())
            }
        }
    }

    private func formatTopItems(_ items: [NamedCount]) -> String {
        guard !items.isEmpty else { return "暂无" }
        return items.prefix(3).map { "\($0.name)(\($0.count))" }.joined(separator: "、")
    }

    private func recommendationText(from snapshot: ReportSnapshot) -> String {
        guard let topFactor = snapshot.topFactors.first else {
            return "建议继续每日记录症状、饮食和压力事件，以便形成可行动的趋势建议。"
        }
        return "当前优先关注「\(topFactor.name)」，并持续 7 天观察它与症状波动的关系。"
    }
}

struct MedicalListView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var selectedRange = 1
    @State private var selectedQuestions: Set<String> = ["我的症状是否可能与围绝经期有关？", "是否需要做相关检查？"]
    @State private var customQuestion = ""
    private let ranges = [7, 30, 90]
    private let baseQuestions = [
        "我的症状是否可能与围绝经期有关？",
        "是否需要做相关检查？",
        "是否适合激素治疗？",
        "我的睡眠问题是否需要进一步评估？",
        "饮食和生活方式有什么建议？"
    ]

    var body: some View {
        let snapshot = vm.reportSnapshot(rangeDays: ranges[selectedRange])

        PageScaffold(title: "就医沟通清单", showBack: true) {
            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    ForEach(["近7天", "近30天", "近90天"].indices, id: \.self) { i in
                        let active = i == selectedRange
                        Text(["近7天", "近30天", "近90天"][i])
                            .font(.system(size: 14, weight: .medium))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Capsule().fill(active ? CATheme.primary : .white.opacity(0.72)))
                            .overlay(Capsule().stroke(active ? .clear : CATheme.border, lineWidth: 1))
                            .foregroundStyle(active ? .white : CATheme.subText)
                            .onTapGesture { selectedRange = i }
                    }
                    Spacer()
                }

                FrostedCard {
                    HStack(spacing: 10) {
                        Circle()
                            .fill(.white.opacity(0.8))
                            .frame(width: 44, height: 44)
                            .overlay(Image(systemName: "checkmark.circle").foregroundStyle(CATheme.primaryAlt))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("数据已准备就绪")
                                .font(.system(size: 15, weight: .semibold))
                            Text("根据你近\(ranges[selectedRange])天的 \(snapshot.sampleCount) 条记录生成")
                                .font(.system(size: 12))
                                .foregroundStyle(CATheme.subText)
                        }
                    }
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("主要症状汇总")
                            .font(.system(size: 15, weight: .bold))
                        if snapshot.topSymptoms.isEmpty {
                            summaryRow("暂无可用记录", "-")
                        } else {
                            ForEach(snapshot.topSymptoms.prefix(3), id: \.name) { item in
                                summaryRow(item.name, "\(item.count) 次")
                            }
                        }
                    }
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("需要特别说明的情况")
                            .font(.system(size: 15, weight: .bold))
                        Text(snapshot.specialReminder ?? "当前暂无高风险提醒，建议继续记录并在就诊时带上趋势摘要。")
                            .font(.system(size: 13))
                            .foregroundStyle(CATheme.text)
                    }
                }

                FlowTags(tags: baseQuestions, selected: $selectedQuestions)

                TextField("添加你自己的问题...", text: $customQuestion)
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 16).fill(.white.opacity(0.74)))

                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("清单预览")
                            .font(.system(size: 14, weight: .bold))
                        Text("症状记录（近\(ranges[selectedRange])天）：\(symptomPreview(from: snapshot))")
                        Text("想咨询医生：\(Array(selectedQuestions).joined(separator: "；"))\(customQuestion.isEmpty ? "" : "；\(customQuestion)")")
                    }
                    .font(.system(size: 13))
                    .foregroundStyle(CATheme.text)
                }

                HStack(spacing: 8) {
                    Button("复制") {}
                    Button("保存") {}
                    Button("保存为图片") {}
                }
                .buttonStyle(PrimaryButtonStyle())
            }
        }
    }

    @ViewBuilder
    private func summaryRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value).foregroundStyle(CATheme.subText)
        }
        .font(.system(size: 13))
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white.opacity(0.72)))
    }

    private func symptomPreview(from snapshot: ReportSnapshot) -> String {
        guard !snapshot.topSymptoms.isEmpty else { return "暂无数据" }
        return snapshot.topSymptoms.prefix(4).map { "\($0.name) \($0.count) 次" }.joined(separator: "、")
    }
}
