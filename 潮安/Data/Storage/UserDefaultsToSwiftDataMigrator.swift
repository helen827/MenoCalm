import Foundation
#if canImport(SwiftData)
import SwiftData

final class UserDefaultsToSwiftDataMigrator {
    private let defaults: UserDefaults
    private let context: ModelContext

    init(defaults: UserDefaults = .standard, context: ModelContext) {
        self.defaults = defaults
        self.context = context
    }

    func migrateIfNeeded(userID: String) throws {
        let markerKey = "chaoan_swiftdata_migration_v1_user_\(userID)"
        if defaults.bool(forKey: markerKey) {
            try backfillCacheIfNeeded(userID: userID)
            return
        }

        try migrateJournalEntries(userID: userID)
        try migrateConversationInsights(userID: userID)
        try migrateReportSnapshots(userID: userID)
        try migratePracticeSummary(userID: userID)

        try context.save()
        defaults.set(true, forKey: markerKey)
    }

    private func backfillCacheIfNeeded(userID: String) throws {
        let journalDescriptor = FetchDescriptor<LocalJournalEntryCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        let insightDescriptor = FetchDescriptor<LocalConversationInsightCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        let reportDescriptor = FetchDescriptor<LocalReportSnapshotCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        let hasJournal = try !context.fetch(journalDescriptor).isEmpty
        let hasInsights = try !context.fetch(insightDescriptor).isEmpty
        let hasReports = try !context.fetch(reportDescriptor).isEmpty
        if !hasJournal {
            try migrateJournalEntries(userID: userID)
        }
        if !hasInsights {
            try migrateConversationInsights(userID: userID)
        }
        if !hasReports {
            try migrateReportSnapshots(userID: userID)
        }
        if !hasJournal || !hasInsights || !hasReports {
            try context.save()
        }
    }

    private func migrateJournalEntries(userID: String) throws {
        let entriesKey = StorageKeys.scoped(StorageKeys.entries, userID: userID)
        guard let data = defaults.data(forKey: entriesKey) else { return }
        let entries = try JSONDecoder().decode([JournalEntry].self, from: data)
        guard !entries.isEmpty else { return }

        for entry in entries {
            let entryJSON = try JSONEncoder().encode(entry)
            context.insert(
                LocalJournalEntryCache(
                    id: entry.id,
                    userID: userID,
                    createdAt: millisToDate(entry.createdAt),
                    entryJSON: entryJSON
                )
            )

            let conversation = LocalAIConversation(
                id: "legacy_conv_\(entry.id)",
                userID: userID,
                createdAt: millisToDate(entry.createdAt),
                updatedAt: millisToDate(entry.createdAt),
                syncStatus: .localOnly,
                syncRevision: entry.revision ?? 0,
                startedAt: millisToDate(entry.createdAt),
                rawTranscript: entry.rawText,
                extractedSymptomsJSON: try? encodeArray(entry.extracted.symptoms),
                extractedTriggersJSON: try? encodeArray(entry.extracted.triggers),
                modelName: "legacy-local"
            )
            context.insert(conversation)

            // Legacy entries only store symptom names. We create one symptom row per item.
            for symptom in entry.extracted.symptoms {
                let symptomRecord = LocalSymptomRecord(
                    id: "legacy_symptom_\(entry.id)_\(symptom)",
                    userID: userID,
                    createdAt: millisToDate(entry.createdAt),
                    updatedAt: millisToDate(entry.createdAt),
                    syncStatus: .localOnly,
                    syncRevision: entry.revision ?? 0,
                    recordDate: millisToDate(entry.createdAt),
                    symptomType: symptom,
                    source: "ai_extracted",
                    sourceConversationID: conversation.id
                )
                context.insert(symptomRecord)
            }
        }
    }

    private func migrateConversationInsights(userID: String) throws {
        let key = StorageKeys.scoped(StorageKeys.insights, userID: userID)
        guard let data = defaults.data(forKey: key) else { return }
        let insights = try JSONDecoder().decode([ConversationInsight].self, from: data)
        for insight in insights {
            let insightJSON = try JSONEncoder().encode(insight)
            context.insert(
                LocalConversationInsightCache(
                    id: insight.id,
                    userID: userID,
                    createdAt: millisToDate(insight.createdAt),
                    insightJSON: insightJSON
                )
            )
        }
    }

    private func migrateReportSnapshots(userID: String) throws {
        let key = StorageKeys.scoped(StorageKeys.reportSnapshots, userID: userID)
        guard let data = defaults.data(forKey: key) else { return }
        let snapshots = try JSONDecoder().decode([ReportSnapshot].self, from: data)
        for snapshot in snapshots {
            let snapshotJSON = try JSONEncoder().encode(snapshot)
            context.insert(
                LocalReportSnapshotCache(
                    id: "\(userID)_\(snapshot.rangeDays)",
                    userID: userID,
                    rangeDays: snapshot.rangeDays,
                    generatedAt: millisToDate(snapshot.generatedAt),
                    snapshotJSON: snapshotJSON
                )
            )
        }
    }

    private func migratePracticeSummary(userID: String) throws {
        let totalKey = StorageKeys.scoped(StorageKeys.practiceTotalSessions, userID: userID)
        let total = defaults.integer(forKey: totalKey)
        guard total > 0 else { return }
        let completedAtValue = defaults.object(
            forKey: StorageKeys.scoped(StorageKeys.practiceLastCompletedAt, userID: userID)
        ) as? TimeInterval

        // Legacy only has summary counters, so we store one synthetic imported row.
        let synthetic = LocalBreathingSession(
            id: "legacy_breathing_summary_\(userID)",
            userID: userID,
            syncStatus: .localOnly,
            sessionType: "legacy-imported-summary",
            plannedDurationSec: total * 180,
            actualDurationSec: total * 180,
            completed: true,
            completedAt: completedAtValue.map { Date(timeIntervalSince1970: $0) },
            source: "migration"
        )
        context.insert(synthetic)
    }

    private func encodeArray(_ values: [String]) throws -> String {
        let data = try JSONEncoder().encode(values)
        return String(decoding: data, as: UTF8.self)
    }

    private func millisToDate(_ value: TimeInterval) -> Date {
        Date(timeIntervalSince1970: value / 1000)
    }
}
#endif
