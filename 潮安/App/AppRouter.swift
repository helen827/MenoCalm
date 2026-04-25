import SwiftUI
import Combine

final class AppRouter: ObservableObject {
    @Published var selectedTab: MainTab = .home
    @Published var path: [AppRoute] = []

    func push(_ route: AppRoute) {
        path.append(route)
    }

    func pop() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func resetToTab(_ tab: MainTab) {
        selectedTab = tab
        path.removeAll()
    }
}
