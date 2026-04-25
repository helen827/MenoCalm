import SwiftUI

struct ContentView: View {
    @StateObject private var vm = AppViewModel()
    @StateObject private var router = AppRouter()

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [CATheme.bg, CATheme.lilac.opacity(0.45), CATheme.bg],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            if vm.isLoggedIn {
                MainShellView()
                    .environmentObject(vm)
                    .environmentObject(router)
            } else {
                OnboardingFlowView()
                    .environmentObject(vm)
            }
        }
    }
}

#Preview {
    ContentView()
}
