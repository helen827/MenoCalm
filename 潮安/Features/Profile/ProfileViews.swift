import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var vm: AppViewModel
    @EnvironmentObject var router: AppRouter
    private let days = Array(1...30)

    var body: some View {
        PageScaffold(title: "我的") {
            VStack(spacing: 12) {
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("每日一言")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(CATheme.subText)
                        Text("你已经在认真照顾自己了，这就很了不起。")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(CATheme.text)
                    }
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("近30天记录日历")
                            .font(.system(size: 15, weight: .bold))
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 7), spacing: 8) {
                            ForEach(days, id: \.self) { day in
                                Text("\(day)")
                                    .font(.system(size: 11, weight: .medium))
                                    .frame(width: 30, height: 30)
                                    .background(day == 21 ? CATheme.primary.opacity(0.9) : (day % 3 == 0 ? CATheme.lilac.opacity(0.8) : .white.opacity(0.7)))
                                    .clipShape(Circle())
                                    .foregroundStyle(CATheme.text)
                            }
                        }
                    }
                }

                FrostedCard {
                    HStack {
                        statItem("打卡", "\(max(vm.journalEntries.count, 7))")
                        statItem("练习", "12")
                        statItem("已读", "24")
                    }
                }

                VStack(spacing: 8) {
                    menuButton("我的趋势报告") { router.push(.trendReport) }
                    menuButton("就医沟通清单") { router.push(.medicalList) }
                    menuButton("我的自测结果") { router.push(.selfTestResult) }
                    menuButton("设置") { router.push(.settings) }
                    menuButton("历史页：状态选择") { router.push(.userStatus) }
                    menuButton("历史页：基础信息") { router.push(.basicInfo) }
                    menuButton("历史页：症状自测") { router.push(.selfTest) }
                }
            }
        }
    }

    @ViewBuilder
    private func statItem(_ title: String, _ value: String) -> some View {
        VStack(spacing: 4) {
            Text(value).font(.system(size: 18, weight: .bold))
            Text(title).font(.system(size: 12)).foregroundStyle(CATheme.subText)
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func menuButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundStyle(CATheme.text)
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(CATheme.subText)
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 18).fill(.white.opacity(0.62)))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

struct SettingsView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var reminderOn = true
    @State private var bedtimeOn = false
    @State private var weeklyOn = true

    var body: some View {
        let snapshot = vm.rolloutDashboardSnapshot()
        let sync = vm.syncDashboardSnapshot()

        PageScaffold(title: "设置", showBack: true) {
            VStack(spacing: 12) {
                FrostedCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("提醒")
                            .font(.system(size: 15, weight: .bold))
                        settingToggle("每日记录提醒", subtitle: "每天 21:00", isOn: $reminderOn)
                        settingToggle("睡前练习提醒", subtitle: "每天 22:00", isOn: $bedtimeOn)
                        settingToggle("周报提醒", subtitle: "每周一 10:00", isOn: $weeklyOn)
                    }
                }
                FrostedCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("数据与隐私")
                            .font(.system(size: 15, weight: .bold))
                        settingArrow("导出我的数据")
                        settingArrow("隐私政策")
                        settingArrow("医疗免责声明")
                    }
                }
                FrostedCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("危险区域")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.red)
                        settingArrow("删除我的数据", color: .red)
                        settingArrow("注销账号", color: .red)
                    }
                }
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("上线监控（内部）")
                            .font(.system(size: 15, weight: .bold))
                        Text("同步成功：\(snapshot.syncSuccess) | 同步失败：\(snapshot.syncFailures)")
                            .font(.system(size: 13))
                        Text("远端读取成功：\(snapshot.remoteReads) | 失败：\(snapshot.remoteFailures)")
                            .font(.system(size: 13))
                        Text("同步失败率：\(percent(snapshot.syncFailureRate)) | 远端失败率：\(percent(snapshot.remoteFailureRate))")
                            .font(.system(size: 12))
                            .foregroundStyle(CATheme.subText)
                        if vm.rolloutAlerts.isEmpty {
                            Text("状态：正常（未触发告警阈值）")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(.green)
                        } else {
                            ForEach(vm.rolloutAlerts, id: \.self) { warning in
                                Text("告警：\(warning)")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                }
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("同步排障（内部）")
                            .font(.system(size: 15, weight: .bold))
                        Text("pending: \(sync.pendingCount) | running: \(sync.runningCount) | retried: \(sync.retriedCount) | failed: \(sync.failedCount)")
                            .font(.system(size: 13))
                        if sync.recent.isEmpty {
                            Text("暂无同步任务轨迹")
                                .font(.system(size: 12))
                                .foregroundStyle(CATheme.subText)
                        } else {
                            ForEach(sync.recent.prefix(4)) { task in
                                Text("#\(task.id.prefix(6)) · \(task.status.rawValue) · 重试\(task.attempts)次")
                                    .font(.system(size: 12))
                                    .foregroundStyle(task.status == .failed ? .red : CATheme.subText)
                            }
                        }
                    }
                }
                Text("潮安 V0.2 MVP")
                    .font(.system(size: 12))
                    .foregroundStyle(CATheme.subText)
            }
        }
    }

    @ViewBuilder
    private func settingToggle(_ title: String, subtitle: String, isOn: Binding<Bool>) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(CATheme.subText)
            }
            Spacer()
            Toggle("", isOn: isOn).labelsHidden().tint(CATheme.primaryAlt)
        }
        .font(.system(size: 14))
    }

    @ViewBuilder
    private func settingArrow(_ title: String, color: Color = CATheme.text) -> some View {
        HStack {
            Text(title).foregroundStyle(color)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundStyle(CATheme.subText)
        }
        .font(.system(size: 14))
    }

    private func percent(_ value: Double) -> String {
        String(format: "%.0f%%", value * 100)
    }
}
