import Foundation

struct JournalEntry: Codable, Identifiable {
    var id: String
    var date: String
    var rawText: String
    var createdAt: TimeInterval
    var revision: Int? = nil
    var extracted: ExtractedData
}

struct ExtractedData: Codable {
    var text: String
    var foods: [String]
    var drinks: [String]
    var work: WorkData
    var symptoms: [String]
    var events: [SymptomEvent]
    var triggers: [String]
}

struct WorkData: Codable {
    var busy: Bool
    var tired: Bool
    var level: Int
}

struct SymptomEvent: Codable {
    var time: String
    var symptom: String
}

struct LifestyleEvent: Codable, Equatable {
    var type: String
    var value: String
}

struct ConversationInsight: Codable, Identifiable, Equatable {
    var id: String
    var sourceText: String
    var createdAt: TimeInterval
    var symptoms: [String]
    var triggers: [String]
    var lifestyleEvents: [LifestyleEvent]
}

struct NamedCount: Codable, Equatable {
    var name: String
    var count: Int
}

struct SymptomFactorCorrelation: Codable, Equatable {
    var symptom: String
    var factor: String
    var count: Int

    var id: String {
        "\(symptom)|\(factor)"
    }
}

struct ReportSnapshot: Codable, Equatable {
    var rangeDays: Int
    var generatedAt: TimeInterval
    var sampleCount: Int
    var topSymptoms: [NamedCount]
    var topFactors: [NamedCount]
    var correlations: [SymptomFactorCorrelation]
    var specialReminder: String?
}

enum StorageKeys {
    static let entries = "chaoan_journal_entries_v1"
    static let latest = "chaoan_journal_latest_v1"
    static let insights = "chaoan_conversation_insights_v1"
    static let reportSnapshots = "chaoan_report_snapshots_v1"
    static let syncQueuePending = "chaoan_sync_queue_pending_v1"
    static let syncQueueFailed = "chaoan_sync_queue_failed_v1"
    static let syncQueueHistory = "chaoan_sync_queue_history_v1"
    static let authTokens = "chaoan_auth_tokens_v1"
    static let practiceTotalSessions = "chaoan_practice_total_sessions_v1"
    static let practiceLastCompletedAt = "chaoan_practice_last_completed_at_v1"

    static func scoped(_ key: String, userID: String) -> String {
        "user_\(userID)_\(key)"
    }
}
