import Foundation

protocol ConversationInsightRepositoryProtocol {
    func loadInsights() -> [ConversationInsight]
    func saveInsights(_ insights: [ConversationInsight])
}
