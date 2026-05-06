import SwiftUI
import UIKit

struct MultilingualTextView: UIViewRepresentable {
    @Binding var text: String
    var font: UIFont = .systemFont(ofSize: 16, weight: .medium)

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.font = font
        textView.keyboardType = .default
        textView.autocorrectionType = .default
        textView.autocapitalizationType = .none
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.isScrollEnabled = true
        textView.text = text
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        // Preserve IME composing text (e.g. Chinese Pinyin) and avoid interrupting marked text.
        guard uiView.markedTextRange == nil else { return }
        if uiView.text != text {
            uiView.text = text
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text)
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        @Binding private var text: String

        init(text: Binding<String>) {
            _text = text
        }

        func textViewDidChange(_ textView: UITextView) {
            text = textView.text ?? ""
        }
    }
}
