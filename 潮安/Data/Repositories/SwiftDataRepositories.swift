import Foundation
#if canImport(SwiftData)
import SwiftData

@Model
final class LocalJournalEntryCache {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var entryJSON: Data

    init(id: String, userID: String, createdAt: Date, entryJSON: Data) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.entryJSON = entryJSON
    }
}

@Model
final class LocalConversationInsightCache {
    @Attribute(.unique) var id: String
    var userID: String
    var createdAt: Date
    var insightJSON: Data

    init(id: String, userID: String, createdAt: Date, insightJSON: Data) {
        self.id = id
        self.userID = userID
        self.createdAt = createdAt
        self.insightJSON = insightJSON
    }
}

@Model
final class LocalReportSnapshotCache {
    @Attribute(.unique) var id: String
    var userID: String
    var rangeDays: Int
    var generatedAt: Date
    var snapshotJSON: Data

    init(id: String, userID: String, rangeDays: Int, generatedAt: Date, snapshotJSON: Data) {
        self.id = id
        self.userID = userID
        self.rangeDays = rangeDays
        self.generatedAt = generatedAt
        self.snapshotJSON = snapshotJSON
    }
}

struct SwiftDataJournalRepository: JournalRepositoryProtocol {
    private let contextProvider: () -> ModelContext?
    private let userIDProvider: () -> String

    init(contextProvider: @escaping () -> ModelContext?, userIDProvider: @escaping () -> String) {
        self.contextProvider = contextProvider
        self.userIDProvider = userIDProvider
    }

    func loadEntries() -> [JournalEntry] {
        guard let context = contextProvider() else { return [] }
        let userID = userIDProvider()
        var descriptor = FetchDescriptor<LocalJournalEntryCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        descriptor.sortBy = [SortDescriptor(\.createdAt, order: .reverse)]
        do {
            let rows = try context.fetch(descriptor)
            return rows.compactMap { try? JSONDecoder().decode(JournalEntry.self, from: $0.entryJSON) }
        } catch {
            return []
        }
    }

    func saveEntries(_ entries: [JournalEntry]) {
        guard let context = contextProvider() else { return }
        let userID = userIDProvider()
        do {
            try context.delete(model: LocalJournalEntryCache.self, where: #Predicate { $0.userID == userID })
            for entry in entries {
                let data = try JSONEncoder().encode(entry)
                context.insert(
                    LocalJournalEntryCache(
                        id: entry.id,
                        userID: userID,
                        createdAt: Date(timeIntervalSince1970: entry.createdAt / 1000),
                        entryJSON: data
                    )
                )
            }
            try context.save()
        } catch {
            return
        }
    }
}

struct SwiftDataConversationInsightRepository: ConversationInsightRepositoryProtocol {
    private let contextProvider: () -> ModelContext?
    private let userIDProvider: () -> String

    init(contextProvider: @escaping () -> ModelContext?, userIDProvider: @escaping () -> String) {
        self.contextProvider = contextProvider
        self.userIDProvider = userIDProvider
    }

    func loadInsights() -> [ConversationInsight] {
        guard let context = contextProvider() else { return [] }
        let userID = userIDProvider()
        var descriptor = FetchDescriptor<LocalConversationInsightCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        descriptor.sortBy = [SortDescriptor(\.createdAt, order: .reverse)]
        do {
            let rows = try context.fetch(descriptor)
            return rows.compactMap { try? JSONDecoder().decode(ConversationInsight.self, from: $0.insightJSON) }
        } catch {
            return []
        }
    }

    func saveInsights(_ insights: [ConversationInsight]) {
        guard let context = contextProvider() else { return }
        let userID = userIDProvider()
        do {
            try context.delete(model: LocalConversationInsightCache.self, where: #Predicate { $0.userID == userID })
            for insight in insights {
                let data = try JSONEncoder().encode(insight)
                context.insert(
                    LocalConversationInsightCache(
                        id: insight.id,
                        userID: userID,
                        createdAt: Date(timeIntervalSince1970: insight.createdAt / 1000),
                        insightJSON: data
                    )
                )
            }
            try context.save()
        } catch {
            return
        }
    }
}

struct SwiftDataReportSnapshotRepository: ReportSnapshotRepositoryProtocol {
    private let contextProvider: () -> ModelContext?
    private let userIDProvider: () -> String

    init(contextProvider: @escaping () -> ModelContext?, userIDProvider: @escaping () -> String) {
        self.contextProvider = contextProvider
        self.userIDProvider = userIDProvider
    }

    func loadSnapshots() -> [ReportSnapshot] {
        guard let context = contextProvider() else { return [] }
        let userID = userIDProvider()
        var descriptor = FetchDescriptor<LocalReportSnapshotCache>(
            predicate: #Predicate { $0.userID == userID }
        )
        descriptor.sortBy = [SortDescriptor(\.rangeDays, order: .forward)]
        do {
            let rows = try context.fetch(descriptor)
            return rows.compactMap { try? JSONDecoder().decode(ReportSnapshot.self, from: $0.snapshotJSON) }
        } catch {
            return []
        }
    }

    func saveSnapshots(_ snapshots: [ReportSnapshot]) {
        guard let context = contextProvider() else { return }
        let userID = userIDProvider()
        do {
            try context.delete(model: LocalReportSnapshotCache.self, where: #Predicate { $0.userID == userID })
            for snapshot in snapshots {
                let data = try JSONEncoder().encode(snapshot)
                context.insert(
                    LocalReportSnapshotCache(
                        id: "\(userID)_\(snapshot.rangeDays)",
                        userID: userID,
                        rangeDays: snapshot.rangeDays,
                        generatedAt: Date(timeIntervalSince1970: snapshot.generatedAt / 1000),
                        snapshotJSON: data
                    )
                )
            }
            try context.save()
        } catch {
            return
        }
    }
}
#endif
