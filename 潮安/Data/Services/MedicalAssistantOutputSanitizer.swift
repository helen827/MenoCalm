import Foundation

/// Post-processes assistant-visible text to enforce Step 6 hard blocks on model/RAG output.
enum MedicalAssistantOutputSanitizer {
    private static let diagnosisOrCareSubstitution =
        "我不能给出诊断结论或替代就医的建议。若你有不适或正在用药，建议尽快联系医生评估；以下为一般性健康科普参考。"

    private static let assistantHardBlockPhrases = [
        "确诊为", "明确诊断为", "你这就是", "你就是", "我确诊你是", "可以确诊",
        "无需就医", "不必看医生", "不需要看医生", "不用去医院", "不用看医生",
        "没必要去医院", "在家就行不用", "不需要治疗"
    ]

    private static let fabricatedEvidencePhrases = [
        "虚构指南", "伪造研究", "不存在的论文"
    ]

    static func sanitize(_ text: String, decision: MedicalSafetyDecision) -> String {
        var t = text
        if containsHardBlock(in: t) || fabricatedEvidencePhrases.contains(where: t.contains) {
            return diagnosisOrCareSubstitution
        }
        if decision.requiresOutputSanitization {
            t = redactPrescriptionLikeLines(t)
            if t.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return diagnosisOrCareSubstitution
            }
        }
        return t
    }

    private static func containsHardBlock(in text: String) -> Bool {
        assistantHardBlockPhrases.contains(where: text.contains)
    }

    private static func redactPrescriptionLikeLines(_ text: String) -> String {
        let lines = text.components(separatedBy: .newlines)
        let kept = lines.filter { !lineLooksLikeDosingOrRx($0) }
        return kept.joined(separator: "\n")
    }

    private static func lineLooksLikeDosingOrRx(_ line: String) -> Bool {
        let t = line
        if t.range(of: "(?i)mg\\b", options: .regularExpression) != nil { return true }
        if t.contains("毫克") { return true }
        if t.contains("微克") { return true }
        if t.contains("剂量") && t.range(of: "\\d", options: .regularExpression) != nil { return true }
        if t.range(of: "服用\\s*\\d", options: .regularExpression) != nil { return true }
        if t.range(of: "口服\\s*\\d", options: .regularExpression) != nil { return true }
        if t.range(of: "每次\\s*\\d", options: .regularExpression) != nil { return true }
        if t.range(of: "\\d+\\s*[～~\\-]\\s*\\d+\\s*(mg|毫克)", options: .regularExpression) != nil { return true }
        if t.contains("一日") && t.range(of: "\\d", options: .regularExpression) != nil && (t.contains("次") || t.contains("片") || t.contains("粒")) {
            return true
        }
        if t.range(of: "\\d+\\s*次\\s*[／/]\\s*日", options: .regularExpression) != nil { return true }
        if t.contains("处方") && (t.contains("如下") || t.contains("建议") || t.contains("按")) { return true }
        if t.contains("按以下") && (t.contains("服用") || t.contains("剂量")) { return true }
        return false
    }
}
