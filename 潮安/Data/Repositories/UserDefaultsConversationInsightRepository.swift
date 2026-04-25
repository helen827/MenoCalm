import Foundation

struct UserDefaultsConversationInsightRepository: ConversationInsightRepositoryProtocol {
    private let defaults: UserDefaults
    private let userIDProvider: () -> String

    init(
        defaults: UserDefaults = .standard,
        userIDProvider: @escaping () -> String = { "guest-local" }
    ) {
        self.defaults = defaults
        self.userIDProvider = userIDProvider
    }

    func loadInsights() -> [ConversationInsight] {
        let key = StorageKeys.scoped(StorageKeys.insights, userID: userIDProvider())
        guard let data = defaults.data(forKey: key) else { return [] }
        do {
            return try JSONDecoder().decode([ConversationInsight].self, from: data)
        } catch {
            print("insight load error: \(error.localizedDescription)")
            return []
        }
    }

    func saveInsights(_ insights: [ConversationInsight]) {
        do {
            let data = try JSONEncoder().encode(insights)
            let key = StorageKeys.scoped(StorageKeys.insights, userID: userIDProvider())
            defaults.set(data, forKey: key)
        } catch {
            print("insight persist error: \(error.localizedDescription)")
        }
    }
}
