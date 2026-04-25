import Foundation

final class RAGService: RAGServiceProtocol {
    private var knowledgeBaseVersion: String
    private var documents: [KnowledgeDocument]

    init(
        knowledgeBaseVersion: String = "kb-v1-local",
        documents: [KnowledgeDocument] = [
            KnowledgeDocument(
                sourceId: "kb-guideline-menopause-basic",
                title: "围绝经期健康管理基础建议",
                keywords: ["潮热", "出汗"],
                response: "潮热发作时可先尝试降低环境温度、减少辛辣和咖啡因摄入，并持续记录出现时段。若频繁影响生活，建议就医评估。"
            ),
            KnowledgeDocument(
                sourceId: "kb-self-care-sleep",
                title: "睡眠与生活方式调整要点",
                keywords: ["失眠", "睡", "睡眠"],
                response: "睡眠相关困扰可先从固定作息、减少晚间刺激和放松呼吸练习开始；若持续数周未改善，建议咨询医生。"
            )
        ]
    ) {
        self.knowledgeBaseVersion = knowledgeBaseVersion
        self.documents = documents
    }

    func generateReply(userText: String) -> RAGAnswer {
        let normalized = userText.trimmingCharacters(in: .whitespacesAndNewlines)
        if let matched = documents.first(where: { doc in
            doc.keywords.contains(where: { normalized.contains($0) })
        }) {
            return RAGAnswer(
                text: matched.response,
                citations: [
                    RAGCitation(
                        sourceId: matched.sourceId,
                        title: matched.title,
                        knowledgeBaseVersion: knowledgeBaseVersion
                    )
                ],
                knowledgeBaseVersion: knowledgeBaseVersion
            )
        } else if documents.isEmpty {
            return RAGAnswer(
                text: "知识库暂不可用，已记录你的情况。请继续补充症状和生活方式信息，我会先做结构化整理。",
                citations: [],
                knowledgeBaseVersion: knowledgeBaseVersion
            )
        } else {
            return RAGAnswer(
                text: "已记录你的情况。你可以继续补充症状出现时段、饮食和压力事件，我会帮助你做趋势整理。",
                citations: [
                    RAGCitation(
                        sourceId: documents[0].sourceId,
                        title: documents[0].title,
                        knowledgeBaseVersion: knowledgeBaseVersion
                    )
                ],
                knowledgeBaseVersion: knowledgeBaseVersion
            )
        }
    }

    func updateKnowledgeBase(documents: [KnowledgeDocument], version: String) {
        self.documents = documents
        self.knowledgeBaseVersion = version
    }

    func currentKnowledgeBaseVersion() -> String {
        knowledgeBaseVersion
    }
}
