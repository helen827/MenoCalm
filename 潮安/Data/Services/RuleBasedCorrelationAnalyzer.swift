import Foundation

struct RuleBasedCorrelationAnalyzer: CorrelationAnalyzerProtocol {
    func analyze(insights: [ConversationInsight], rangeDays: Int, now: TimeInterval) -> ReportSnapshot {
        let dayMillis: TimeInterval = 24 * 60 * 60 * 1000
        let cutoff = now - (Double(rangeDays) * dayMillis)
        let filtered = insights.filter { $0.createdAt >= cutoff }

        var symptomCounter: [String: Int] = [:]
        var factorCounter: [String: Int] = [:]
        var pairCounter: [String: Int] = [:]

        for insight in filtered {
            let symptoms = Array(Set(insight.symptoms))
            let factors = Array(
                Set(
                    insight.triggers
                    + insight.lifestyleEvents.map { "\($0.type):\($0.value)" }
                )
            )

            symptoms.forEach { symptomCounter[$0, default: 0] += 1 }
            factors.forEach { factorCounter[$0, default: 0] += 1 }

            for symptom in symptoms {
                for factor in factors {
                    pairCounter["\(symptom)|\(factor)", default: 0] += 1
                }
            }
        }

        let topSymptoms = symptomCounter
            .sorted { lhs, rhs in
                if lhs.value == rhs.value { return lhs.key < rhs.key }
                return lhs.value > rhs.value
            }
            .prefix(5)
            .map { NamedCount(name: $0.key, count: $0.value) }

        let topFactors = factorCounter
            .sorted { lhs, rhs in
                if lhs.value == rhs.value { return lhs.key < rhs.key }
                return lhs.value > rhs.value
            }
            .prefix(5)
            .map { NamedCount(name: $0.key, count: $0.value) }

        let correlations = pairCounter
            .compactMap { key, count -> SymptomFactorCorrelation? in
                let parts = key.split(separator: "|", maxSplits: 1).map(String.init)
                guard parts.count == 2 else { return nil }
                return SymptomFactorCorrelation(symptom: parts[0], factor: parts[1], count: count)
            }
            .sorted { lhs, rhs in
                if lhs.count == rhs.count {
                    if lhs.symptom == rhs.symptom { return lhs.factor < rhs.factor }
                    return lhs.symptom < rhs.symptom
                }
                return lhs.count > rhs.count
            }
            .prefix(5)
            .map { $0 }

        let reminder: String?
        if let palpitations = symptomCounter["心悸"], palpitations > 0 {
            reminder = "近\(rangeDays)天记录到\(palpitations)次心悸，建议就医时主动说明发生时段与伴随症状。"
        } else {
            reminder = nil
        }

        return ReportSnapshot(
            rangeDays: rangeDays,
            generatedAt: now,
            sampleCount: filtered.count,
            topSymptoms: topSymptoms,
            topFactors: topFactors,
            correlations: correlations,
            specialReminder: reminder
        )
    }
}
