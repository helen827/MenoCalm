import Foundation

final class RedactingConversationAuditor: ConversationAuditorProtocol {
    private(set) var events: [ConversationAuditEvent] = []

    func record(
        userText: String,
        assistantReply: String,
        decision: MedicalSafetyDecision
    ) {
        let event = ConversationAuditEvent(
            id: UUID().uuidString,
            timestamp: Date(),
            userText: redactPII(in: userText),
            assistantReply: redactPII(in: assistantReply),
            riskLevel: decision.riskLevel,
            shouldBlockResponse: decision.shouldBlockResponse
        )
        events.append(event)
    }

    private func redactPII(in text: String) -> String {
        var redacted = text
        redacted = applyingRegexReplace(
            pattern: "(?<!\\d)1\\d{10}(?!\\d)",
            replacement: "[已脱敏手机号]",
            in: redacted
        )
        return redacted
    }

    private func applyingRegexReplace(
        pattern: String,
        replacement: String,
        in text: String,
        options: NSRegularExpression.Options = []
    ) -> String {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: options) else {
            return text
        }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return regex.stringByReplacingMatches(in: text, options: [], range: range, withTemplate: replacement)
    }
}
