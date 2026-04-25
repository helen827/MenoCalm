import Foundation

struct ConversationAuditEvent: Equatable {
    let id: String
    let timestamp: Date
    let userText: String
    let assistantReply: String
    let riskLevel: MedicalRiskLevel
    let shouldBlockResponse: Bool
}

protocol ConversationAuditorProtocol {
    func record(
        userText: String,
        assistantReply: String,
        decision: MedicalSafetyDecision
    )
}
