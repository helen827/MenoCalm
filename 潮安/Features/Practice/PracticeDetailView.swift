import SwiftUI
import Combine

struct PracticeDetailView: View {
    @StateObject private var session = PracticeBreathingSession()
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
            }
            .onReceive(timer) { _ in session.tick() }
        }
    }
}
