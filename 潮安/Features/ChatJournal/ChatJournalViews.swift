import SwiftUI

struct HomeView: View {
    private struct ChatMessage: Identifiable {
        let id = UUID()
        let text: String
        let isUser: Bool
    }

    @EnvironmentObject var vm: AppViewModel
    @State private var inputText = ""
    @State private var messages: [ChatMessage] = [
        .init(text: "你可以直接和我说今天做了什么、有什么症状、心情怎么样。我会自动帮你归纳到报告里。", isUser: false),
        .init(text: "也可以随时问我任何更年期相关科普问题，比如潮热、睡眠、情绪波动、饮食和运动。", isUser: false)
    ]
    @State private var showVoiceHint = false
    @State private var isSending = false
    @State private var sendErrorMessage: String?
    @State private var retryDraft: String?

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    aiBadge
                        .padding(.top, 10)
#if DEBUG
                    backendDebugBadge
#endif

                    ForEach(messages) { message in
                        messageBubble(message)
                    }

                    if let sendErrorMessage {
                        FrostedCard {
                            HStack(alignment: .top, spacing: 10) {
                                Image(systemName: "wifi.exclamationmark")
                                    .foregroundStyle(.orange)
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(sendErrorMessage)
                                        .font(.system(size: 13, weight: .medium))
                                    if retryDraft != nil {
                                        Button("重试发送") {
                                            retryLastMessage()
                                        }
                                        .buttonStyle(.plain)
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundStyle(CATheme.primaryAlt)
                                        .disabled(isSending)
                                    }
                                }
                                Spacer(minLength: 0)
                            }
                        }
                    }

                    quickActions
                        .padding(.top, 2)
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 14)
            }
            .onChange(of: messages.count) { _, _ in
                if let lastID = messages.last?.id {
                    withAnimation(.easeOut(duration: 0.2)) {
                        proxy.scrollTo(lastID, anchor: .bottom)
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                ChatComposerBar(
                    text: $inputText,
                    isSending: isSending,
                    onAttach: {},
                    onVoice: { showVoiceHint = true },
                    onSend: { sendMessage() }
                )
            }
        }
        .background(
            LinearGradient(
                colors: [CATheme.bg, Color(hex: 0xEBF0F8)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .alert("语音暂不可用", isPresented: $showVoiceHint) {
            Button("知道了", role: .cancel) {}
        } message: {
            Text("当前为原型版本，已降级为文本输入。")
        }
    }

    private var aiBadge: some View {
        Text("潮安AI小棉袄")
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(CATheme.subText)
            .padding(.horizontal, 13)
            .padding(.vertical, 7)
            .background(Capsule().fill(.white.opacity(0.82)))
            .frame(maxWidth: .infinity)
    }

#if DEBUG
    private var backendDebugBadge: some View {
        Text("后端命中：\(vm.aiBackendEndpointDebug)")
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(CATheme.subText)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Capsule().fill(.white.opacity(0.72)))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
#endif

    private func messageBubble(_ message: ChatMessage) -> some View {
        HStack {
            if message.isUser {
                Spacer(minLength: 52)
                Text(message.text)
                    .font(.system(size: 16, weight: .semibold))
                    .lineSpacing(2)
                    .foregroundStyle(CATheme.text)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(CATheme.primary.opacity(0.7))
                    )
            } else {
                Text(message.text)
                    .font(.system(size: 16, weight: .semibold))
                    .lineSpacing(2)
                    .foregroundStyle(CATheme.text)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(.white.opacity(0.86))
                    )
                Spacer(minLength: 52)
            }
        }
        .id(message.id)
    }

    private var quickActions: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 128), spacing: 8)], alignment: .leading, spacing: 8) {
            quickChip("我是不是围绝经期？") { inputText = "我是不是围绝经期？" }
            quickChip("潮热怎么快速缓解？") { inputText = "潮热怎么快速缓解？" }
            quickChip("今晚怎么睡得更好？") { inputText = "今晚怎么睡得更好？" }
        }
    }

    @ViewBuilder
    private func quickChip(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(CATheme.subText)
                .padding(.horizontal, 14)
                .padding(.vertical, 9)
                .background(Capsule().fill(.white.opacity(0.82)))
                .overlay(Capsule().stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func sendMessage() {
        performSend(
            content: inputText,
            shouldAppendUserMessage: true,
            shouldPersistJournal: true
        )
    }

    private func retryLastMessage() {
        guard let retryDraft else { return }
        performSend(
            content: retryDraft,
            shouldAppendUserMessage: false,
            shouldPersistJournal: false
        )
    }

    private func performSend(content: String, shouldAppendUserMessage: Bool, shouldPersistJournal: Bool) {
        let content = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty else { return }
        guard !isSending else { return }
        isSending = true
        sendErrorMessage = nil

        let safety = vm.evaluateConversationSafety(userText: content)
        if shouldAppendUserMessage {
            messages.append(.init(text: content, isUser: true))
        }
        if shouldPersistJournal {
            vm.saveJournal(text: content)
        }

        let assistantReply: String
        if safety.shouldBlockResponse, let safeReply = safety.safeReply {
            assistantReply = safeReply
        } else {
            let history = messages.map(\.text)
            assistantReply = vm.generateGuidedAssistantReply(userText: content, history: history, safety: safety)
        }
        let normalizedReply = assistantReply.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedReply.isEmpty else {
            sendErrorMessage = "消息发送失败，请检查网络后重试。"
            retryDraft = content
            isSending = false
            return
        }
        messages.append(.init(text: assistantReply, isUser: false))
        vm.auditConversation(userText: content, assistantReply: assistantReply, decision: safety)
        inputText = ""
        retryDraft = nil
        isSending = false
    }
}
