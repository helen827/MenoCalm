import Foundation

enum OperationalEventLevel: String, Codable {
    case info
    case warning
    case critical
}

struct OperationalEvent: Codable, Identifiable {
    var id: String
    var category: String
    var message: String
    var level: OperationalEventLevel
    var createdAt: TimeInterval
}

struct OperationalAlert: Codable, Identifiable {
    var id: String
    var title: String
    var detail: String
    var level: OperationalEventLevel
    var createdAt: TimeInterval
}

protocol AlertNotifierProtocol {
    func notify(alert: OperationalAlert)
}

final class ConsoleAlertNotifier: AlertNotifierProtocol {
    func notify(alert: OperationalAlert) {
        print("[alert][\(alert.level.rawValue)] \(alert.title): \(alert.detail)")
    }
}

final class OperationalAlertingCenter {
    private(set) var events: [OperationalEvent] = []
    private(set) var alerts: [OperationalAlert] = []
    private let notifier: AlertNotifierProtocol

    init(notifier: AlertNotifierProtocol = ConsoleAlertNotifier()) {
        self.notifier = notifier
    }

    func record(category: String, message: String, level: OperationalEventLevel = .info) {
        let event = OperationalEvent(
            id: UUID().uuidString,
            category: category,
            message: message,
            level: level,
            createdAt: Date().timeIntervalSince1970
        )
        events.append(event)
        if events.count > 200 {
            events.removeFirst(events.count - 200)
        }
        if level == .critical || level == .warning {
            emitAlert(from: event)
        }
    }

    func evaluateRollout(_ snapshot: RolloutSnapshot) {
        if snapshot.syncFailureRate > 0.15 {
            record(
                category: "sync",
                message: "同步失败率偏高：\(percent(snapshot.syncFailureRate))",
                level: .warning
            )
        }
        if snapshot.remoteFailureRate > 0.10 {
            record(
                category: "remote-read",
                message: "远端读取失败率偏高：\(percent(snapshot.remoteFailureRate))",
                level: .warning
            )
        }
    }

    private func emitAlert(from event: OperationalEvent) {
        let alert = OperationalAlert(
            id: UUID().uuidString,
            title: "运行告警：\(event.category)",
            detail: event.message,
            level: event.level,
            createdAt: event.createdAt
        )
        alerts.append(alert)
        if alerts.count > 60 {
            alerts.removeFirst(alerts.count - 60)
        }
        notifier.notify(alert: alert)
    }

    private func percent(_ value: Double) -> String {
        String(format: "%.0f%%", value * 100)
    }
}
