import Foundation

enum PrivacyActionType: String, Codable {
    case export
    case deleteData
    case deactivateAccount
}

enum PrivacyActionStatus: String, Codable {
    case success
    case failure
}

struct PrivacyActionAuditRecord: Codable, Identifiable {
    var id: String
    var action: PrivacyActionType
    var status: PrivacyActionStatus
    var userID: String
    var detail: String
    var createdAt: TimeInterval
}

final class PrivacyActionAuditStore {
    private let defaults: UserDefaults
    private let storageKey = "chaoan_privacy_action_audit_v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func append(action: PrivacyActionType, status: PrivacyActionStatus, userID: String, detail: String) {
        var records = loadAll()
        records.append(
            PrivacyActionAuditRecord(
                id: UUID().uuidString,
                action: action,
                status: status,
                userID: userID,
                detail: detail,
                createdAt: Date().timeIntervalSince1970
            )
        )
        if records.count > 100 {
            records.removeFirst(records.count - 100)
        }
        save(records)
    }

    func recent(limit: Int = 20) -> [PrivacyActionAuditRecord] {
        Array(loadAll().suffix(limit).reversed())
    }

    private func loadAll() -> [PrivacyActionAuditRecord] {
        guard let data = defaults.data(forKey: storageKey) else { return [] }
        return (try? JSONDecoder().decode([PrivacyActionAuditRecord].self, from: data)) ?? []
    }

    private func save(_ records: [PrivacyActionAuditRecord]) {
        if let data = try? JSONEncoder().encode(records) {
            defaults.set(data, forKey: storageKey)
        }
    }
}
