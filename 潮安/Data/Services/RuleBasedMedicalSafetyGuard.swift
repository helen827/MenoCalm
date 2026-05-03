import Foundation

struct RuleBasedMedicalSafetyGuard: MedicalSafetyGuardProtocol {
    private static let highRiskKeywords = [
        "胸痛", "胸闷", "呼吸困难", "喘不过气", "憋气", "晕厥", "昏迷", "大出血",
        "自杀", "不想活", "伤害自己", "轻生", "自残", "服毒", "过量服药",
        "呕血", "吐血", "中风", "心梗", "心肌梗死", "意识不清", "意识丧失",
        "抽搐", "濒死", "濒死感"
    ]

    private static let mediumRiskKeywords = [
        "心悸", "严重失眠", "严重焦虑", "焦虑加重", "恐慌发作", "惊恐发作",
        "抑郁加重", "重度抑郁", "持续出血", "剧烈头痛", "高热不退", "严重过敏"
    ]

    private static let delayCarePhrases = [
        "观察几天", "先观察几天", "先在家观察", "在家观察几天", "不用去医院",
        "没必要去医院", "先别去医院", "再等几天看"
    ]

    private static let delayCareRedFlags = [
        "胸痛", "胸闷", "呼吸困难", "喘不过气", "憋气", "大出血", "呕血", "吐血",
        "晕厥", "昏迷", "中风", "心梗", "心肌梗死", "意识不清", "意识丧失"
    ]

    private static let prescriptionIntentPhrases = [
        "吃什么药", "吃哪种药", "开什么药", "开个药", "给我开药", "开个处方",
        "处方", "吃多少", "吃几片", "一天吃几次", "每日几次", "剂量多少", "用量多少",
        "该怎么吃药", "推荐药", "推荐用药", "吃什么药好"
    ]

    private static let mediumTemplate =
        "我可以先帮你整理记录并给出一般性健康建议；若症状持续或加重，建议尽快就医评估。"

    private static let prescriptionPreamble =
        "我不能代替医生或药师开具处方或给出个体化剂量。下面仅为一般性健康科普，用药与剂量请务必线下确认。"

    private static let highSafeReply =
        "你提到的情况可能存在较高风险，建议尽快联系专业医生或急救服务。潮安可继续帮助你整理近期症状记录，便于就医沟通。"

    func evaluate(userText: String) -> MedicalSafetyDecision {
        if impliesDelayOfUrgentCare(userText: userText)
            || Self.highRiskKeywords.contains(where: userText.contains) {
            return MedicalSafetyDecision(
                riskLevel: .high,
                shouldBlockResponse: true,
                safeReply: Self.highSafeReply,
                decisionPath: impliesDelayOfUrgentCare(userText: userText)
                    ? "high:delay_urgent_care"
                    : "high:keyword"
            )
        }

        if Self.prescriptionIntentPhrases.contains(where: userText.contains) {
            return MedicalSafetyDecision(
                riskLevel: .medium,
                shouldBlockResponse: false,
                replyPreamble: Self.prescriptionPreamble + "\n\n",
                requiresOutputSanitization: true,
                decisionPath: "medium:prescription_intent"
            )
        }

        if Self.mediumRiskKeywords.contains(where: userText.contains) {
            return MedicalSafetyDecision(
                riskLevel: .medium,
                shouldBlockResponse: false,
                replyPreamble: Self.mediumTemplate + "\n\n",
                decisionPath: "medium:symptom_keyword"
            )
        }

        return MedicalSafetyDecision(
            riskLevel: .low,
            shouldBlockResponse: false,
            decisionPath: "low:default"
        )
    }

    private func impliesDelayOfUrgentCare(userText: String) -> Bool {
        let urgesDelay = Self.delayCarePhrases.contains(where: userText.contains)
        let redFlag = Self.delayCareRedFlags.contains(where: userText.contains)
        return urgesDelay && redFlag
    }
}
