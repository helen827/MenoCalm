import Foundation

struct RuleBasedMedicalSafetyGuard: MedicalSafetyGuardProtocol {
    private static let highRiskKeywords = [
        "胸痛", "胸闷", "呼吸困难", "晕厥", "昏迷", "大出血", "自杀", "不想活", "伤害自己", "濒死"
    ]

    private static let mediumRiskKeywords = [
        "心悸", "严重失眠", "抑郁", "焦虑", "持续出血", "剧烈头痛"
    ]

    func evaluate(userText: String) -> MedicalSafetyDecision {
        if Self.highRiskKeywords.contains(where: userText.contains) {
            return MedicalSafetyDecision(
                riskLevel: .high,
                shouldBlockResponse: true,
                safeReply: "你提到的情况可能存在较高风险，建议尽快联系专业医生或急救服务。潮安可继续帮助你整理近期症状记录，便于就医沟通。"
            )
        }

        if Self.mediumRiskKeywords.contains(where: userText.contains) {
            return MedicalSafetyDecision(
                riskLevel: .medium,
                shouldBlockResponse: false,
                safeReply: "我可以先帮你整理记录并给出一般性健康建议；若症状持续或加重，建议尽快就医评估。"
            )
        }

        return MedicalSafetyDecision(
            riskLevel: .low,
            shouldBlockResponse: false,
            safeReply: nil
        )
    }
}
