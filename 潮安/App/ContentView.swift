import SwiftUI
#if canImport(SwiftData)
import SwiftData
#endif

struct ContentView: View {
    @StateObject private var vm = AppViewModel()
    @StateObject private var router = AppRouter()
#if canImport(SwiftData)
    @Environment(\.modelContext) private var modelContext
#endif

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
        .alert("登录失效", isPresented: $vm.showSessionExpiredAlert) {
            Button("知道了", role: .cancel) {}
        } message: {
            Text(vm.sessionExpiredMessage)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if !vm.isNetworkReachable {
                HStack(spacing: 8) {
                    Image(systemName: "wifi.slash")
                        .font(.system(size: 14, weight: .semibold))
                    Text("当前无网络，在线内容与同步可能不可用；本地记录仍可使用。")
                        .font(.system(size: 13, weight: .medium))
                    Spacer(minLength: 0)
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color.orange.opacity(0.92))
            }
        }
#if canImport(SwiftData)
        .task {
            vm.bindLocalModelContext(modelContext)
        }
#endif
    }
}

#Preview {
    ContentView()
}
