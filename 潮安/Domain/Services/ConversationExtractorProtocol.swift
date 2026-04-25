import Foundation

protocol ConversationExtractorProtocol {
    func extractInsight(
        from text: String,
        createdAt: TimeInterval,
        extracted: ExtractedData
    ) -> ConversationInsight
}
