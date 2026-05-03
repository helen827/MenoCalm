import Foundation
#if canImport(SwiftData)
import SwiftData

enum LocalSyncStatus: String, Codable {
    case localOnly
    case pendingUpload
    case synced
    case failed
}

@Model
final class LocalUserProfile {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var phoneMasked: String?
    var nickname: String?
    var birthYear: Int?
    var menopauseStage: String?
    var preferredReminderTime: String?
    var guidedConversationEnabled: Bool

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        phoneMasked: String? = nil,
        nickname: String? = nil,
        birthYear: Int? = nil,
        menopauseStage: String? = nil,
        preferredReminderTime: String? = nil,
        guidedConversationEnabled: Bool = true
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.phoneMasked = phoneMasked
        self.nickname = nickname
        self.birthYear = birthYear
        self.menopauseStage = menopauseStage
        self.preferredReminderTime = preferredReminderTime
        self.guidedConversationEnabled = guidedConversationEnabled
    }
}

@Model
final class LocalSymptomRecord {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var recordDate: Date
    var symptomType: String
    var severity: Int?
    var note: String?
    var source: String
    var sourceConversationID: String?

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        recordDate: Date = Date(),
        symptomType: String,
        severity: Int? = nil,
        note: String? = nil,
        source: String = "manual",
        sourceConversationID: String? = nil
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.recordDate = recordDate
        self.symptomType = symptomType
        self.severity = severity
        self.note = note
        self.source = source
        self.sourceConversationID = sourceConversationID
    }
}

@Model
final class LocalMoodRecord {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var recordDate: Date
    var moodLabel: String
    var score: Int?
    var note: String?
    var source: String

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        recordDate: Date = Date(),
        moodLabel: String,
        score: Int? = nil,
        note: String? = nil,
        source: String = "manual"
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.recordDate = recordDate
        self.moodLabel = moodLabel
        self.score = score
        self.note = note
        self.source = source
    }
}

@Model
final class LocalSleepRecord {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var sleepDate: Date
    var durationMinutes: Int?
    var qualityScore: Int?
    var nightWakeCount: Int?
    var note: String?

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        sleepDate: Date = Date(),
        durationMinutes: Int? = nil,
        qualityScore: Int? = nil,
        nightWakeCount: Int? = nil,
        note: String? = nil
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.sleepDate = sleepDate
        self.durationMinutes = durationMinutes
        self.qualityScore = qualityScore
        self.nightWakeCount = nightWakeCount
        self.note = note
    }
}

@Model
final class LocalHotFlashRecord {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var occurredAt: Date
    var intensity: Int?
    var durationMinutes: Int?
    var triggerHint: String?
    var note: String?

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        occurredAt: Date = Date(),
        intensity: Int? = nil,
        durationMinutes: Int? = nil,
        triggerHint: String? = nil,
        note: String? = nil
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.occurredAt = occurredAt
        self.intensity = intensity
        self.durationMinutes = durationMinutes
        self.triggerHint = triggerHint
        self.note = note
    }
}

@Model
final class LocalAIConversation {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var startedAt: Date
    var endedAt: Date?
    var title: String?
    var rawTranscript: String
    var riskLevel: String?
    var safetyDecision: String?
    var extractedSymptomsJSON: String?
    var extractedTriggersJSON: String?
    var modelName: String?
    var knowledgeBaseVersion: String?

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        startedAt: Date = Date(),
        endedAt: Date? = nil,
        title: String? = nil,
        rawTranscript: String,
        riskLevel: String? = nil,
        safetyDecision: String? = nil,
        extractedSymptomsJSON: String? = nil,
        extractedTriggersJSON: String? = nil,
        modelName: String? = nil,
        knowledgeBaseVersion: String? = nil
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.title = title
        self.rawTranscript = rawTranscript
        self.riskLevel = riskLevel
        self.safetyDecision = safetyDecision
        self.extractedSymptomsJSON = extractedSymptomsJSON
        self.extractedTriggersJSON = extractedTriggersJSON
        self.modelName = modelName
        self.knowledgeBaseVersion = knowledgeBaseVersion
    }
}

@Model
final class LocalContentBookmark {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var contentID: String
    var contentType: String
    var title: String?
    var bookmarkedAt: Date
    var lastReadAt: Date?
    var readProgress: Double?

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        contentID: String,
        contentType: String,
        title: String? = nil,
        bookmarkedAt: Date = Date(),
        lastReadAt: Date? = nil,
        readProgress: Double? = nil
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.contentID = contentID
        self.contentType = contentType
        self.title = title
        self.bookmarkedAt = bookmarkedAt
        self.lastReadAt = lastReadAt
        self.readProgress = readProgress
    }
}

@Model
final class LocalBreathingSession {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var sessionType: String
    var plannedDurationSec: Int
    var actualDurationSec: Int
    var completed: Bool
    var completedAt: Date?
    var source: String

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        sessionType: String,
        plannedDurationSec: Int,
        actualDurationSec: Int,
        completed: Bool,
        completedAt: Date? = nil,
        source: String = "manual"
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.sessionType = sessionType
        self.plannedDurationSec = plannedDurationSec
        self.actualDurationSec = actualDurationSec
        self.completed = completed
        self.completedAt = completedAt
        self.source = source
    }
}

@Model
final class LocalConsentRecord {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    var syncStatusRaw: String
    var syncRevision: Int
    var serverID: String?
    var lastSyncedAt: Date?

    var consentType: String
    var version: String
    var accepted: Bool
    var acceptedAt: Date?
    var entryPoint: String

    init(
        id: String = UUID().uuidString,
        userID: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil,
        syncStatus: LocalSyncStatus = .localOnly,
        syncRevision: Int = 0,
        serverID: String? = nil,
        lastSyncedAt: Date? = nil,
        consentType: String,
        version: String,
        accepted: Bool,
        acceptedAt: Date? = nil,
        entryPoint: String
    ) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.syncRevision = syncRevision
        self.serverID = serverID
        self.lastSyncedAt = lastSyncedAt
        self.consentType = consentType
        self.version = version
        self.accepted = accepted
        self.acceptedAt = acceptedAt
        self.entryPoint = entryPoint
    }
}

enum LocalSchemaV1 {
    static let allModels: [any PersistentModel.Type] = [
        LocalUserProfile.self,
        LocalSymptomRecord.self,
        LocalMoodRecord.self,
        LocalSleepRecord.self,
        LocalHotFlashRecord.self,
        LocalAIConversation.self,
        LocalContentBookmark.self,
        LocalBreathingSession.self,
        LocalConsentRecord.self,
        LocalJournalEntryCache.self,
        LocalConversationInsightCache.self,
        LocalReportSnapshotCache.self
    ]
}
#endif
