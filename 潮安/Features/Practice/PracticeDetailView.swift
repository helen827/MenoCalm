import SwiftUI
import Combine

struct PracticeDetailView: View {
    @EnvironmentObject var vm: AppViewModel
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var session = PracticeBreathingSession()
    @State private var completionRecorded = false
    @State private var showResetConfirm = false
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        PageScaffold(title: "呼吸详情", showBack: true) {
            VStack(spacing: 18) {
                Text("剩余时间 \(session.formatMMSS())")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(CATheme.subText)

                ZStack {
                    Circle()
                        .stroke(CATheme.border.opacity(0.5), lineWidth: 10)
                        .frame(width: 238, height: 238)
                    Circle()
                        .trim(from: 0, to: session.progress())
                        .stroke(CATheme.primary, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                        .frame(width: 238, height: 238)
                        .rotationEffect(.degrees(-90))
                    Circle()
                        .fill(LinearGradient(colors: [CATheme.primary.opacity(0.9), CATheme.primaryAlt.opacity(0.85)], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: session.phase == "吸气" ? 148 : (session.phase == "屏息" ? 168 : 126),
                               height: session.phase == "吸气" ? 148 : (session.phase == "屏息" ? 168 : 126))
                        .animation(.easeInOut(duration: 0.9), value: session.phase)
                    VStack {
                        Image(systemName: "wind")
                            .font(.system(size: 24))
                        Text(session.phase).font(.system(size: 24, weight: .bold))
                        Text("\(session.phaseRemaining)s").font(.system(size: 14, weight: .medium))
                    }
                    .foregroundStyle(.white)
                }

                HStack(spacing: 8) {
                    Image(systemName: session.phaseIcon())
                    Text("\(session.phase) \(session.phaseRemaining) 秒")
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(CATheme.subText)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Capsule().fill(.white.opacity(0.65)))

                Button(session.isPlaying ? "暂停" : "继续") {
                    session.togglePlay()
                }
                .buttonStyle(PrimaryButtonStyle())
                .frame(width: 140)

                Button("重新开始本轮") {
                    showResetConfirm = true
                }
                .buttonStyle(SecondaryButtonStyle())
                .font(.system(size: 14, weight: .medium))

                FrostedCard {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "info.circle")
                        Text("若倒计时看起来卡住，可点「重新开始本轮」。切换到其他应用时会自动暂停，回来点「继续」即可。")
                            .font(.system(size: 12))
                            .foregroundStyle(CATheme.subText)
                    }
                }
            }
            .onReceive(timer) { _ in session.tick() }
            .onChange(of: session.sessionRemaining) { _, newValue in
                if newValue == 0, !completionRecorded {
                    completionRecorded = true
                    vm.recordBreathingSessionCompleted()
                }
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .background || phase == .inactive {
                    if session.isPlaying {
                        session.togglePlay()
                    }
                }
            }
            .alert("重新开始练习？", isPresented: $showResetConfirm) {
                Button("取消", role: .cancel) {}
                Button("重新开始", role: .destructive) {
                    session.resetSession()
                    completionRecorded = false
                }
            } message: {
                Text("将重置本轮倒计时，不会重复计入已完成次数。")
            }
        }
    }
}
