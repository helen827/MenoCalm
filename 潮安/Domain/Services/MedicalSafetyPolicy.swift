import Foundation

enum MedicalRiskLevel: Equatable {
    case low
    case medium
    case high
}

struct MedicalSafetyDecision: Equatable {
    let riskLevel: MedicalRiskLevel
    let shouldBlockResponse: Bool
    /// Full replacement reply when `shouldBlockResponse` is true (e.g. high risk).
    let safeReply: String?
    /// Prepended before guided / RAG content for medium-risk framing (and similar).
    let replyPreamble: String?
    let requiresOutputSanitization: Bool
    /// Short label for audit (e.g. `high:keyword`, `medium:prescription_intent`).
    let decisionPath: String

    init(
        riskLevel: MedicalRiskLevel,
        shouldBlockResponse: Bool,
        safeReply: String? = nil,
        replyPreamble: String? = nil,
        requiresOutputSanitization: Bool = false,
        decisionPath: String = ""
    ) {
        self.riskLevel = riskLevel
        self.shouldBlockResponse = shouldBlockResponse
        self.safeReply = safeReply
        self.replyPreamble = replyPreamble
        self.requiresOutputSanitization = requiresOutputSanitization
        self.decisionPath = decisionPath
    }
}

protocol MedicalSafetyGuardProtocol {
    func evaluate(userText: String) -> MedicalSafetyDecision
}
