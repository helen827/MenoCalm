import Foundation

struct UserDefaultsReportSnapshotRepository: ReportSnapshotRepositoryProtocol {
    private let defaults: UserDefaults
    private let userIDProvider: () -> String

    init(
        defaults: UserDefaults = .standard,
        userIDProvider: @escaping () -> String = { "guest-local" }
    ) {
        self.defaults = defaults
        self.userIDProvider = userIDProvider
    }

    func loadSnapshots() -> [ReportSnapshot] {
        let key = StorageKeys.scoped(StorageKeys.reportSnapshots, userID: userIDProvider())
        guard let data = defaults.data(forKey: key) else { return [] }
        do {
            return try JSONDecoder().decode([ReportSnapshot].self, from: data)
        } catch {
            print("report snapshot load error: \(error.localizedDescription)")
            return []
        }
    }

    func saveSnapshots(_ snapshots: [ReportSnapshot]) {
        do {
            let data = try JSONEncoder().encode(snapshots)
            let key = StorageKeys.scoped(StorageKeys.reportSnapshots, userID: userIDProvider())
            defaults.set(data, forKey: key)
        } catch {
            print("report snapshot persist error: \(error.localizedDescription)")
        }
    }
}
