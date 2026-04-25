import Foundation

enum SyncTaskStatus: String, Codable {
    case pending
    case running
    case retried
    case failed
    case succeeded
}

struct SyncUploadTask: Codable {
    let id: String
    var attempts: Int
    var status: SyncTaskStatus
    var lastError: String?
    var updatedAt: TimeInterval
    let entries: [JournalEntry]

    enum CodingKeys: String, CodingKey {
        case id
        case attempts
        case status
        case lastError
        case updatedAt
        case entries
    }

    init(id: String, attempts: Int, status: SyncTaskStatus, lastError: String?, updatedAt: TimeInterval, entries: [JournalEntry]) {
        self.id = id
        self.attempts = attempts
        self.status = status
        self.lastError = lastError
        self.updatedAt = updatedAt
        self.entries = entries
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        attempts = try container.decode(Int.self, forKey: .attempts)
        status = try container.decodeIfPresent(SyncTaskStatus.self, forKey: .status) ?? .pending
        lastError = try container.decodeIfPresent(String.self, forKey: .lastError)
        updatedAt = try container.decodeIfPresent(TimeInterval.self, forKey: .updatedAt) ?? 0
        entries = try container.decode([JournalEntry].self, forKey: .entries)
    }
}

struct SyncTaskTrace: Codable, Identifiable {
    let id: String
    var status: SyncTaskStatus
    var attempts: Int
    var updatedAt: TimeInterval
    var errorSummary: String?
}

struct SyncQueueDiagnostics {
    var pendingCount: Int
    var runningCount: Int
    var retriedCount: Int
    var failedCount: Int
    var recent: [SyncTaskTrace]
}

final class SyncQueue {
    private(set) var pendingTasks: [SyncUploadTask] = []
    private(set) var failedJobIDs: [String] = []
    private(set) var taskHistory: [SyncTaskTrace] = []
    private let maxRetries: Int
    private let defaults: UserDefaults
    private let userIDProvider: () -> String

    init(
        maxRetries: Int = 3,
        defaults: UserDefaults = .standard,
        userIDProvider: @escaping () -> String = { "guest-local" }
    ) {
        self.maxRetries = maxRetries
        self.defaults = defaults
        self.userIDProvider = userIDProvider
        self.pendingTasks = loadPendingTasks()
        self.failedJobIDs = loadFailedIDs()
        self.taskHistory = loadHistory()
    }

    func enqueueUpload(entries: [JournalEntry], id: String = UUID().uuidString) {
        let now = Date().timeIntervalSince1970
        pendingTasks.append(
            SyncUploadTask(
                id: id,
                attempts: 0,
                status: .pending,
                lastError: nil,
                updatedAt: now,
                entries: entries
            )
        )
        recordHistory(id: id, status: .pending, attempts: 0, updatedAt: now, errorSummary: nil)
        persist()
    }

    func flush(uploader: ([JournalEntry]) throws -> Void) {
        var remaining: [SyncUploadTask] = []

        for var task in pendingTasks {
            task.status = .running
            task.updatedAt = Date().timeIntervalSince1970
            recordHistory(
                id: task.id,
                status: .running,
                attempts: task.attempts,
                updatedAt: task.updatedAt,
                errorSummary: task.lastError
            )
            do {
                try uploader(task.entries)
                recordHistory(
                    id: task.id,
                    status: .succeeded,
                    attempts: task.attempts,
                    updatedAt: Date().timeIntervalSince1970,
                    errorSummary: nil
                )
            } catch {
                task.attempts += 1
                task.updatedAt = Date().timeIntervalSince1970
                task.lastError = String(describing: error)
                if task.attempts >= maxRetries {
                    task.status = .failed
                    failedJobIDs.append(task.id)
                    recordHistory(
                        id: task.id,
                        status: .failed,
                        attempts: task.attempts,
                        updatedAt: task.updatedAt,
                        errorSummary: task.lastError
                    )
                } else {
                    task.status = .retried
                    recordHistory(
                        id: task.id,
                        status: .retried,
                        attempts: task.attempts,
                        updatedAt: task.updatedAt,
                        errorSummary: task.lastError
                    )
                    remaining.append(task)
                }
            }
        }

        pendingTasks = remaining
        persist()
    }

    private func persist() {
        let userID = userIDProvider()
        let pendingKey = StorageKeys.scoped(StorageKeys.syncQueuePending, userID: userID)
        let failedKey = StorageKeys.scoped(StorageKeys.syncQueueFailed, userID: userID)
        let historyKey = StorageKeys.scoped(StorageKeys.syncQueueHistory, userID: userID)
        if let data = try? JSONEncoder().encode(pendingTasks) {
            defaults.set(data, forKey: pendingKey)
        }
        defaults.set(failedJobIDs, forKey: failedKey)
        if let historyData = try? JSONEncoder().encode(taskHistory) {
            defaults.set(historyData, forKey: historyKey)
        }
    }

    private func loadPendingTasks() -> [SyncUploadTask] {
        let key = StorageKeys.scoped(StorageKeys.syncQueuePending, userID: userIDProvider())
        guard let data = defaults.data(forKey: key) else { return [] }
        return (try? JSONDecoder().decode([SyncUploadTask].self, from: data)) ?? []
    }

    private func loadFailedIDs() -> [String] {
        let key = StorageKeys.scoped(StorageKeys.syncQueueFailed, userID: userIDProvider())
        return defaults.stringArray(forKey: key) ?? []
    }

    private func loadHistory() -> [SyncTaskTrace] {
        let key = StorageKeys.scoped(StorageKeys.syncQueueHistory, userID: userIDProvider())
        guard let data = defaults.data(forKey: key) else { return [] }
        return (try? JSONDecoder().decode([SyncTaskTrace].self, from: data)) ?? []
    }

    private func recordHistory(
        id: String,
        status: SyncTaskStatus,
        attempts: Int,
        updatedAt: TimeInterval,
        errorSummary: String?
    ) {
        taskHistory.append(
            SyncTaskTrace(
                id: id,
                status: status,
                attempts: attempts,
                updatedAt: updatedAt,
                errorSummary: errorSummary
            )
        )
        if taskHistory.count > 120 {
            taskHistory.removeFirst(taskHistory.count - 120)
        }
    }

    func diagnostics(limit: Int = 12) -> SyncQueueDiagnostics {
        let pending = pendingTasks.filter { $0.status == .pending }.count
        let running = pendingTasks.filter { $0.status == .running }.count
        let retried = pendingTasks.filter { $0.status == .retried }.count
        return SyncQueueDiagnostics(
            pendingCount: pending,
            runningCount: running,
            retriedCount: retried,
            failedCount: failedJobIDs.count,
            recent: Array(taskHistory.suffix(limit).reversed())
        )
    }
}
