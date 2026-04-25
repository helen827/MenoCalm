import Foundation

struct RuleBasedConversationExtractor: ConversationExtractorProtocol {
    func extractInsight(
        from text: String,
        createdAt: TimeInterval,
        extracted: ExtractedData
    ) -> ConversationInsight {
        var lifestyleEvents: [LifestyleEvent] = []

        extracted.drinks.forEach { drink in
            lifestyleEvents.append(LifestyleEvent(type: "饮品", value: drink))
        }

        if extracted.work.busy {
            lifestyleEvents.append(LifestyleEvent(type: "工作压力", value: "忙碌等级\(extracted.work.level)"))
        }

        if extracted.work.tired {
            lifestyleEvents.append(LifestyleEvent(type: "体感", value: "疲惫"))
        }

        return ConversationInsight(
            id: String(Int(createdAt)),
            sourceText: text,
            createdAt: createdAt,
            symptoms: extracted.symptoms,
            triggers: extracted.triggers,
            lifestyleEvents: lifestyleEvents
        )
    }
}
