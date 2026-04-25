import Foundation
import Combine

final class PracticeBreathingSession: ObservableObject {
    @Published var isPlaying = true
    @Published var sessionRemaining = 300
    @Published var phase = "吸气"
    @Published var phaseRemaining = 4

    private var phaseIndex = 0
    private let phases: [(name: String, sec: Int)] = [("吸气", 4), ("屏息", 7), ("呼气", 8)]

    func togglePlay() {
        isPlaying.toggle()
    }

    func tick() {
        guard isPlaying else { return }
        guard sessionRemaining > 0 else { return }
        sessionRemaining -= 1
        phaseRemaining -= 1

        if phaseRemaining <= 0 {
            phaseIndex = (phaseIndex + 1) % phases.count
            phase = phases[phaseIndex].name
            phaseRemaining = phases[phaseIndex].sec
        }
    }

    func formatMMSS() -> String {
        let s = max(0, sessionRemaining)
        return "\(s / 60):\(String(format: "%02d", s % 60))"
    }

    func progress() -> CGFloat {
        CGFloat(sessionRemaining) / 300
    }

    func phaseIcon() -> String {
        if phase == "吸气" { return "arrow.up" }
        if phase == "屏息" { return "pause" }
        return "arrow.down"
    }
}
