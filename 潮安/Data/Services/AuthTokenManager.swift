import Foundation

protocol AuthTokenProviderProtocol {
    func validAccessToken(userID: String) -> String?
    func refreshAccessToken(userID: String) -> String?
    func invalidate(userID: String)
}

struct StoredAuthToken: Codable {
    var accessToken: String
    var refreshToken: String
    var expiresAt: TimeInterval
}

final class AuthTokenManager: AuthTokenProviderProtocol {
    private let defaults: UserDefaults
    private let nowProvider: () -> TimeInterval

    init(defaults: UserDefaults = .standard, nowProvider: @escaping () -> TimeInterval = {
        Date().timeIntervalSince1970
    }) {
        self.defaults = defaults
        self.nowProvider = nowProvider
    }

    func validAccessToken(userID: String) -> String? {
        guard let token = loadToken(userID: userID) else { return nil }
        if token.expiresAt > nowProvider() {
            return token.accessToken
        }
        return refreshAccessToken(userID: userID)
    }

    func refreshAccessToken(userID: String) -> String? {
        guard var token = loadToken(userID: userID) else { return nil }
        guard !token.refreshToken.isEmpty else {
            invalidate(userID: userID)
            return nil
        }
        // Simulate refresh contract for prototype: derive a new token and extend expiry.
        token.accessToken = "acc-\(userID)-\(Int(nowProvider()))"
        token.expiresAt = nowProvider() + 3600
        saveToken(token, userID: userID)
        return token.accessToken
    }

    func invalidate(userID: String) {
        let key = StorageKeys.scoped(StorageKeys.authTokens, userID: userID)
        defaults.removeObject(forKey: key)
    }

    func seedToken(userID: String, accessToken: String, refreshToken: String, expiresIn: TimeInterval = 3600) {
        let token = StoredAuthToken(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresAt: nowProvider() + expiresIn
        )
        saveToken(token, userID: userID)
    }

    private func saveToken(_ token: StoredAuthToken, userID: String) {
        let key = StorageKeys.scoped(StorageKeys.authTokens, userID: userID)
        if let data = try? JSONEncoder().encode(token) {
            defaults.set(data, forKey: key)
        }
    }

    private func loadToken(userID: String) -> StoredAuthToken? {
        let key = StorageKeys.scoped(StorageKeys.authTokens, userID: userID)
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(StoredAuthToken.self, from: data)
    }
}
