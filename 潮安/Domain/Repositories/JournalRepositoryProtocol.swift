import Foundation

protocol JournalRepositoryProtocol {
    func loadEntries() -> [JournalEntry]
    func saveEntries(_ entries: [JournalEntry])
}
