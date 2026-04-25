import Foundation

struct JournalConflictResolver {
    func resolve(local: [JournalEntry], remote: [JournalEntry]) -> [JournalEntry] {
        var localMap: [String: JournalEntry] = [:]
        var remoteMap: [String: JournalEntry] = [:]
        local.forEach { localMap[$0.id] = $0 }
        remote.forEach { remoteMap[$0.id] = $0 }

        let allIDs = Set(localMap.keys).union(remoteMap.keys)
        let merged = allIDs.compactMap { id -> JournalEntry? in
            let left = localMap[id]
            let right = remoteMap[id]
            switch (left, right) {
            case let (.some(localEntry), .some(remoteEntry)):
                return merge(local: localEntry, remote: remoteEntry)
            case let (.some(localEntry), .none):
                return localEntry
            case let (.none, .some(remoteEntry)):
                return remoteEntry
            case (.none, .none):
                return nil
            }
        }

        return merged.sorted { $0.createdAt > $1.createdAt }
    }

    private func merge(local: JournalEntry, remote: JournalEntry) -> JournalEntry {
        if let localRevision = local.revision, let remoteRevision = remote.revision, localRevision != remoteRevision {
            return localRevision > remoteRevision ? local : remote
        }

        let newer: JournalEntry
        let older: JournalEntry
        if local.createdAt >= remote.createdAt {
            newer = local
            older = remote
        } else {
            newer = remote
            older = local
        }

        return JournalEntry(
            id: newer.id,
            date: newer.date,
            rawText: preferredText(primary: newer.rawText, fallback: older.rawText),
            createdAt: max(local.createdAt, remote.createdAt),
            revision: max(local.revision ?? 0, remote.revision ?? 0),
            extracted: mergeExtracted(newer: newer.extracted, older: older.extracted)
        )
    }

    private func mergeExtracted(newer: ExtractedData, older: ExtractedData) -> ExtractedData {
        ExtractedData(
            text: preferredText(primary: newer.text, fallback: older.text),
            foods: mergeUnique(primary: newer.foods, secondary: older.foods),
            drinks: mergeUnique(primary: newer.drinks, secondary: older.drinks),
            work: WorkData(
                busy: newer.work.busy || older.work.busy,
                tired: newer.work.tired || older.work.tired,
                level: max(newer.work.level, older.work.level)
            ),
            symptoms: mergeUnique(primary: newer.symptoms, secondary: older.symptoms),
            events: mergeEvents(primary: newer.events, secondary: older.events),
            triggers: mergeUnique(primary: newer.triggers, secondary: older.triggers)
        )
    }

    private func preferredText(primary: String, fallback: String) -> String {
        if !primary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return primary
        }
        return fallback
    }

    private func mergeUnique(primary: [String], secondary: [String]) -> [String] {
        var seen: Set<String> = []
        var result: [String] = []
        for value in primary + secondary {
            if seen.insert(value).inserted {
                result.append(value)
            }
        }
        return result
    }

    private func mergeEvents(primary: [SymptomEvent], secondary: [SymptomEvent]) -> [SymptomEvent] {
        var seen: Set<String> = []
        var result: [SymptomEvent] = []
        for event in primary + secondary {
            let key = "\(event.time)|\(event.symptom)"
            if seen.insert(key).inserted {
                result.append(event)
            }
        }
        return result
    }
}
