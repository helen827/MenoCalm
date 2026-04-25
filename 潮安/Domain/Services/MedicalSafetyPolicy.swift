import Foundation

enum MedicalRiskLevel: Equatable {
    case low
    case medium
    case high
}

struct MedicalSafetyDecision: Equatable {
    let riskLevel: MedicalRiskLevel
    let shouldBlockResponse: Bool
    let safeReply: String?
}

protocol MedicalSafetyGuardProtocol {
    func evaluate(userText: String) -> MedicalSafetyDecision
}
