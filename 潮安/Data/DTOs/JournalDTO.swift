import Foundation

struct JournalEntryDTO: Codable {
    var id: String
    var date: String
    var rawText: String
    var createdAt: TimeInterval
    var revision: Int?
    var extracted: ExtractedData
}

extension JournalEntryDTO {
    init(entry: JournalEntry) {
        self.id = entry.id
        self.date = entry.date
        self.rawText = entry.rawText
        self.createdAt = entry.createdAt
        self.revision = entry.revision
        self.extracted = entry.extracted
    }

    var toDomain: JournalEntry {
        JournalEntry(
            id: id,
            date: date,
            rawText: rawText,
            createdAt: createdAt,
            revision: revision,
            extracted: extracted
        )
    }
}
