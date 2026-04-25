import SwiftUI

struct HomeView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var inputText = ""
    @State private var messages: [String] = ["你可以直接和我说今天做了什么、有什么症状、心情怎么样。我会自动帮你归纳到报告里。", "也可以随时问我任何更年期相关科普问题，比如潮热、睡眠、情绪波动、饮食和运动。"]
    @State private var showVoiceHint = false

    var body: some View {
        PageScaffold(title: "AI对话") {
            VStack(spacing: 12) {
                FrostedCard {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "sparkles")
                            .foregroundStyle(CATheme.primaryAlt)
                        VStack(alignment: .leading, spacing: 6) {
                            Text("昨日报告已更新（12:00）")
                                .font(.system(size: 14, weight: .semibold))
                            Text("核心结论：你记录中的潮热出现频次下降，建议继续减少下午咖啡因摄入。")
                                .font(.system(size: 13))
                                .foregroundStyle(CATheme.subText)
                        }
                    }
                }

                FrostedCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("潮安AI小棉袄")
                            .caText(.caption)
                            .foregroundStyle(CATheme.subText)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Capsule().fill(.white.opacity(0.7)))
                        ForEach(messages.indices, id: \.self) { idx in
                            HStack(alignment: .top) {
                                if idx % 2 == 0 {
                                    Text("AI")
                                        .font(.system(size: 11, weight: .bold))
                                        .padding(6)
                                        .background(Capsule().fill(CATheme.blue.opacity(0.5)))
                                    Text(messages[idx])
                                        .caText(.body)
                                        .foregroundStyle(CATheme.text)
                                    Spacer()
                                } else {
                                    Spacer()
                                    Text(messages[idx])
                                        .caText(.body)
                                        .padding(10)
                                        .background(RoundedRectangle(cornerRadius: 14).fill(CATheme.primary.opacity(0.28)))
                                }
                            }
                        }

                        HStack(spacing: 8) {
                            TextField("输入今天的记录...", text: $inputText)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 10)
                                .background(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(.white.opacity(0.7))
                                )
                            Button {
                                showVoiceHint = true
                            } label: {
                                Image(systemName: "mic")
                                    .foregroundStyle(CATheme.text)
                                    .frame(width: 40, height: 40)
                                    .background(Circle().fill(CATheme.lilac))
                            }
                            Button("发送") {
                                sendMessage()
                            }
                            .buttonStyle(PrimaryButtonStyle())
                            .frame(width: 82)
                        }

                        HStack {
                            quickChip("我是不是围绝经期？") {}
                            quickChip("潮热怎么快速缓解？") {}
                            quickChip("今晚怎么睡得更好？") {}
                        }
                    }
                }
            }
            .alert("语音暂不可用", isPresented: $showVoiceHint) {
                Button("知道了", role: .cancel) {}
            } message: {
                Text("当前为原型版本，已降级为文本输入。")
            }
        }
    }

    @ViewBuilder
    private func quickChip(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .caText(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Capsule().fill(CATheme.lilac.opacity(0.85)))
                .foregroundStyle(CATheme.text)
        }
        .buttonStyle(.plain)
    }

    private func sendMessage() {
        let content = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty else { return }

        let safety = vm.evaluateConversationSafety(userText: content)
        messages.append(content)
        vm.saveJournal(text: content)

        let assistantReply: String
        if safety.shouldBlockResponse, let safeReply = safety.safeReply {
            assistantReply = safeReply
        } else if let caution = safety.safeReply {
            assistantReply = caution
        } else {
            let ragAnswer = vm.generateRAGAssistantReply(userText: content)
            assistantReply = ragAnswer.text + formatCitations(ragAnswer.citations, version: ragAnswer.knowledgeBaseVersion)
        }
        messages.append(assistantReply)
        vm.auditConversation(userText: content, assistantReply: assistantReply, decision: safety)
        inputText = ""
    }

    private func formatCitations(_ citations: [RAGCitation], version: String) -> String {
        guard !citations.isEmpty else { return "\n\n知识库版本：\(version)" }
        let titles = citations.map(\.title).joined(separator: "；")
        return "\n\n参考来源：\(titles)\n知识库版本：\(version)"
    }
}
