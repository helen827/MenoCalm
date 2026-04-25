import Foundation

protocol ExtractSignalsUseCaseProtocol {
    func execute(text: String) -> ExtractedData
}

struct RuleBasedExtractSignalsUseCase: ExtractSignalsUseCaseProtocol {
    func execute(text: String) -> ExtractedData {
        let drinks = ["咖啡", "浓茶", "酒"].filter { text.contains($0) }
        let symptoms = ["潮热", "盗汗", "心悸", "失眠", "情绪波动", "焦虑"].filter { text.contains($0) }
        let triggers = [
            text.contains("咖啡") || text.contains("浓茶") ? "咖啡因" : nil,
            text.contains("开会") || text.contains("赶") || text.contains("忙") ? "压力事件" : nil,
            text.contains("辣") ? "辛辣饮食" : nil
        ].compactMap { $0 }

        let level = text.contains("非常忙") ? 3 : (text.contains("忙") ? 2 : 1)
        return ExtractedData(
            text: text,
            foods: [],
            drinks: drinks,
            work: WorkData(busy: level > 1, tired: text.contains("累"), level: level),
            symptoms: symptoms,
            events: symptoms.map { SymptomEvent(time: "今日", symptom: $0) },
            triggers: triggers
        )
    }
}
