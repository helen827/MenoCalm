import Foundation

struct UserDefaultsJournalRepository: JournalRepositoryProtocol {
    private let defaults: UserDefaults
    private let userIDProvider: () -> String

    init(
        defaults: UserDefaults = .standard,
        userIDProvider: @escaping () -> String = { "guest-local" }
    ) {
        self.defaults = defaults
        self.userIDProvider = userIDProvider
    }

    func loadEntries() -> [JournalEntry] {
        let scopedEntriesKey = StorageKeys.scoped(StorageKeys.entries, userID: userIDProvider())
        let scopedLatestKey = StorageKeys.scoped(StorageKeys.latest, userID: userIDProvider())

        if let data = defaults.data(forKey: scopedEntriesKey) {
            return decodeEntries(from: data)
        }

        // Migration fallback from pre-user-scope storage.
        guard let legacyData = defaults.data(forKey: StorageKeys.entries) else { return [] }
        let entries = decodeEntries(from: legacyData)
        defaults.set(legacyData, forKey: scopedEntriesKey)
        if let legacyLatestData = defaults.data(forKey: StorageKeys.latest) {
            defaults.set(legacyLatestData, forKey: scopedLatestKey)
        }
        return entries
    }

    func saveEntries(_ entries: [JournalEntry]) {
        do {
            let data = try JSONEncoder().encode(entries)
            let scopedEntriesKey = StorageKeys.scoped(StorageKeys.entries, userID: userIDProvider())
            let scopedLatestKey = StorageKeys.scoped(StorageKeys.latest, userID: userIDProvider())
            defaults.set(data, forKey: scopedEntriesKey)
            if let latest = entries.first {
                let latestData = try JSONEncoder().encode(latest)
                defaults.set(latestData, forKey: scopedLatestKey)
            }
        } catch {
            print("persist error: \(error.localizedDescription)")
        }
    }

    private func decodeEntries(from data: Data) -> [JournalEntry] {
        do {
            return try JSONDecoder().decode([JournalEntry].self, from: data)
        } catch {
            print("load error: \(error.localizedDescription)")
            return []
        }
    }
}
