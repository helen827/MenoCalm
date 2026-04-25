import SwiftUI

struct IntroPageView: View {
    @EnvironmentObject var vm: AppViewModel
    let index: Int

    private var title: String {
        switch index {
        case 1: "先看懂更年期"
        case 2: "每天记录，自动追踪"
        default: "科普、社区、呼吸练习"
        }
    }

    private var subtitle: String {
        switch index {
        case 1: "潮安用清晰易懂的内容，帮你理解身体变化，减少“是不是我有问题”的焦虑。"
        case 2: "和 AI 对话就能记录症状、情绪和生活方式，系统会自动归纳并生成报告趋势。"
        default: "你可以获得可靠知识、同伴支持，并通过呼吸冥想与生活方式建议逐步改善状态。"
        }
    }

    private var icon: String {
        switch index {
        case 1: "book.closed"
        case 2: "chart.line.uptrend.xyaxis"
        default: "sparkles"
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            IOSStatusBar()
            ZStack {
                LinearGradient(
                    colors: [Color(hex: 0xFCF9FA), Color(hex: 0xF5EEF1)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack {
                    HStack {
                        Spacer()
                        Button("跳过") { vm.onboardingStep = 4 }
                            .font(.system(size: 12))
                            .foregroundStyle(CATheme.subText.opacity(0.7))
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 4)

                    Spacer()

                    ZStack {
                        if index == 1 {
                            FloatingSymptomChip("潮热", icon: "flame")
                                .offset(x: -118, y: -108)
                            FloatingSymptomChip("漏尿", icon: "drop")
                                .offset(x: 102, y: -86)
                            FloatingSymptomChip("脑雾", icon: "cloud")
                                .offset(x: -96, y: -42)
                            FloatingSymptomChip("心悸", icon: "heart")
                                .offset(x: 98, y: -8)
                            FloatingSymptomChip("失眠", icon: "bed.double")
                                .offset(x: 0, y: 44)
                        }

                        RoundedRectangle(cornerRadius: 28)
                            .fill(.white.opacity(0.86))
                            .frame(width: 112, height: 112)
                            .overlay(Image(systemName: icon).font(.system(size: 42)).foregroundStyle(CATheme.primaryAlt))
                            .shadow(color: .black.opacity(0.08), radius: 14, x: 0, y: 8)
                    }
                    .padding(.bottom, 36)

                    Text(title)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundStyle(CATheme.text)
                        .multilineTextAlignment(.center)

                    Text(subtitle)
                        .caText(.body)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                        .foregroundStyle(CATheme.subText)
                        .padding(.horizontal, 34)
                        .padding(.top, 10)

                    Spacer()

                    HStack(spacing: 8) {
                        ForEach(1...3, id: \.self) { i in
                            Circle()
                                .fill(i == index ? CATheme.primaryAlt : CATheme.border)
                                .frame(width: 7, height: 7)
                        }
                    }
                    .padding(.bottom, 8)

                    Text(index == 1 ? "左滑继续" : "右滑返回，左滑继续")
                        .font(.system(size: 11))
                        .foregroundStyle(CATheme.subText.opacity(0.65))

                    HStack(spacing: 12) {
                        if index > 1 {
                            Button("上一步") { vm.onboardingStep -= 1 }
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(CATheme.subText)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .background(RoundedRectangle(cornerRadius: 22).fill(.white.opacity(0.72)))
                        }
                        Button(index == 3 ? "进入潮安" : "下一步") {
                            if index == 3 { vm.onboardingStep = 4 } else { vm.nextOnboarding() }
                        }
                        .buttonStyle(PrimaryButtonStyle())
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 12)
                    .padding(.bottom, 24)
                }
            }
        }
    }
}
