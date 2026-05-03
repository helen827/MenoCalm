import SwiftUI

/// AI 对话底部输入区（与 `HomeView` 解耦，便于维护与复用）
struct ChatComposerBar: View {
    @Binding var text: String
    var isSending: Bool
    var onAttach: () -> Void
    var onVoice: () -> Void
    var onSend: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            iconCircle(systemName: "plus", action: onAttach)

            HStack(spacing: 8) {
                TextField("可以和我说任何事...", text: $text)
                    .font(.system(size: 16, weight: .medium))
                Spacer(minLength: 0)
                Button(action: onVoice) {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(CATheme.subText)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 21, style: .continuous)
                    .fill(.white.opacity(0.92))
            )

            Button(action: onSend) {
                if isSending {
                    ProgressView()
                        .tint(CATheme.text)
                } else {
                    Image(systemName: "paperplane.fill")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(CATheme.text)
                }
            }
            .frame(width: 40, height: 40)
            .background(Circle().fill(CATheme.primary))
            .disabled(isSending)
        }
        .padding(.horizontal, 16)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(.white.opacity(0.68))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(CATheme.border.opacity(0.6))
                .frame(height: 1)
        }
    }

    private func iconCircle(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(CATheme.subText)
                .frame(width: 40, height: 40)
                .background(Circle().fill(.white.opacity(0.8)))
                .overlay(Circle().stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
