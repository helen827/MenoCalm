import SwiftUI

struct OnboardingFlowView: View {
    @EnvironmentObject var vm: AppViewModel

    var body: some View {
        if vm.onboardingStep <= 3 {
            IntroPageView(index: vm.onboardingStep)
        } else {
            WelcomeView()
        }
    }
}
