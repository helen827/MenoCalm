import Foundation
import Combine

final class FeatureFlags: ObservableObject {
    @Published var cloudReadEnabled = false
    @Published var cloudSyncEnabled = false
    @Published var failOpenToLocalData = true
    @Published var guidedConversationEnabled = true
}
