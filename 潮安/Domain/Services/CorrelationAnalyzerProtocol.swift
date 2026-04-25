import Foundation

protocol CorrelationAnalyzerProtocol {
    func analyze(insights: [ConversationInsight], rangeDays: Int, now: TimeInterval) -> ReportSnapshot
}
