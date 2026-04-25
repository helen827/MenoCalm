import Foundation

struct RAGCitation: Equatable {
    let sourceId: String
    let title: String
    let knowledgeBaseVersion: String
}

struct RAGAnswer: Equatable {
    let text: String
    let citations: [RAGCitation]
    let knowledgeBaseVersion: String
}

struct KnowledgeDocument: Codable, Equatable {
    let sourceId: String
    let title: String
    let keywords: [String]
    let response: String
}

protocol RAGServiceProtocol {
    func generateReply(userText: String) -> RAGAnswer
    func updateKnowledgeBase(documents: [KnowledgeDocument], version: String)
    func currentKnowledgeBaseVersion() -> String
}
