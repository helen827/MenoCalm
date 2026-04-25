import Foundation

struct RemoteJournalRepository: JournalRepositoryProtocol {
    private let apiClient: RemoteJournalAPIClientProtocol
    private let userIDProvider: () -> String

    init(
        apiClient: RemoteJournalAPIClientProtocol,
        userIDProvider: @escaping () -> String
    ) {
        self.apiClient = apiClient
        self.userIDProvider = userIDProvider
    }

    func loadEntries() -> [JournalEntry] {
        do {
            return try apiClient.fetchEntries(userID: userIDProvider()).map(\.toDomain)
        } catch {
            return []
        }
    }

    func saveEntries(_ entries: [JournalEntry]) {
        do {
            try apiClient.uploadEntries(entries.map(JournalEntryDTO.init), userID: userIDProvider())
        } catch {
            print("remote persist error: \(error)")
        }
    }

    func uploadOrThrow(_ entries: [JournalEntry]) throws {
        try apiClient.uploadEntries(entries.map(JournalEntryDTO.init), userID: userIDProvider())
    }

    func fetchOrThrow() throws -> [JournalEntry] {
        try apiClient.fetchEntries(userID: userIDProvider()).map(\.toDomain)
    }
}
