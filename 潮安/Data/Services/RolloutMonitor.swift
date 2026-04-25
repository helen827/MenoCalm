import Foundation

struct RolloutThresholds {
    var maxSyncFailureRate: Double
    var maxRemoteFailureRate: Double

    static let `default` = RolloutThresholds(
        maxSyncFailureRate: 0.20,
        maxRemoteFailureRate: 0.30
    )
}

struct RolloutSnapshot {
    let localReads: Int
    let remoteReads: Int
    let remoteFailures: Int
    let syncSuccess: Int
    let syncFailures: Int

    var syncFailureRate: Double {
        let total = syncSuccess + syncFailures
        guard total > 0 else { return 0 }
        return Double(syncFailures) / Double(total)
    }

    var remoteFailureRate: Double {
        let total = remoteReads + remoteFailures
        guard total > 0 else { return 0 }
        return Double(remoteFailures) / Double(total)
    }
}

final class RolloutMonitor {
    private(set) var localReads = 0
    private(set) var remoteReads = 0
    private(set) var remoteFailures = 0
    private(set) var syncSuccess = 0
    private(set) var syncFailures = 0

    func markLocalRead() {
        localReads += 1
    }

    func markRemoteRead() {
        remoteReads += 1
    }

    func markRemoteFailure() {
        remoteFailures += 1
    }

    func markSyncSuccess() {
        syncSuccess += 1
    }

    func markSyncFailure() {
        syncFailures += 1
    }

    func snapshot() -> RolloutSnapshot {
        RolloutSnapshot(
            localReads: localReads,
            remoteReads: remoteReads,
            remoteFailures: remoteFailures,
            syncSuccess: syncSuccess,
            syncFailures: syncFailures
        )
    }

    func alerts(thresholds: RolloutThresholds = .default) -> [String] {
        let data = snapshot()
        var warnings: [String] = []
        if data.syncFailureRate > thresholds.maxSyncFailureRate {
            warnings.append("同步失败率过高：\(percent(data.syncFailureRate)) > \(percent(thresholds.maxSyncFailureRate))")
        }
        if data.remoteFailureRate > thresholds.maxRemoteFailureRate {
            warnings.append("远端读取失败率过高：\(percent(data.remoteFailureRate)) > \(percent(thresholds.maxRemoteFailureRate))")
        }
        return warnings
    }

    private func percent(_ value: Double) -> String {
        String(format: "%.0f%%", value * 100)
    }
}
