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
    private let authAPIClient: AuthAPIClientProtocol
    var onSessionInvalidated: ((String) -> Void)?

    init(
        defaults: UserDefaults = .standard,
        nowProvider: @escaping () -> TimeInterval = { Date().timeIntervalSince1970 },
        authAPIClient: AuthAPIClientProtocol = InMemoryAuthAPIClient()
    ) {
        self.defaults = defaults
        self.nowProvider = nowProvider
        self.authAPIClient = authAPIClient
    }

    func validAccessToken(userID: String) -> String? {
        guard let token = loadToken(userID: userID) else { return nil }
        if token.expiresAt > nowProvider() {
            return token.accessToken
        }
        return refreshAccessToken(userID: userID)
    }

    func refreshAccessToken(userID: String) -> String? {
        guard let token = loadToken(userID: userID) else { return nil }
        guard !token.refreshToken.isEmpty else {
            invalidate(userID: userID)
            return nil
        }
        do {
            let payload = try authAPIClient.refreshToken(token.refreshToken, userID: userID)
            let updated = StoredAuthToken(
                accessToken: payload.accessToken,
                refreshToken: payload.refreshToken,
                expiresAt: nowProvider() + payload.expiresIn
            )
            saveToken(updated, userID: userID)
            return updated.accessToken
        } catch {
            invalidate(userID: userID)
            return nil
        }
    }

    func invalidate(userID: String) {
        let key = StorageKeys.scoped(StorageKeys.authTokens, userID: userID)
        defaults.removeObject(forKey: key)
        onSessionInvalidated?(userID)
    }

    func seedToken(userID: String, accessToken: String, refreshToken: String, expiresIn: TimeInterval = 3600) {
        let token = StoredAuthToken(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresAt: nowProvider() + expiresIn
        )
        saveToken(token, userID: userID)
    }

    func sendPhoneCode(_ phone: String) -> Bool {
        do {
            try authAPIClient.sendPhoneCode(phone)
            return true
        } catch {
            return false
        }
    }

    func loginWithPhoneCode(_ phone: String, code: String) -> String? {
        do {
            let payload = try authAPIClient.verifyPhoneCode(phone, code: code)
            saveAuthPayload(payload)
            return payload.userID
        } catch {
            return nil
        }
    }

    func loginWithPhone(_ phone: String) -> String? {
        do {
            let payload = try authAPIClient.loginWithPhone(phone)
            saveAuthPayload(payload)
            return payload.userID
        } catch {
            return nil
        }
    }

    func loginWithWeChatCode(_ code: String) -> String? {
        do {
            let payload = try authAPIClient.loginWithWeChatCode(code)
            saveAuthPayload(payload)
            return payload.userID
        } catch {
            return nil
        }
    }

    private func saveAuthPayload(_ payload: AuthTokenPayload) {
        let token = StoredAuthToken(
            accessToken: payload.accessToken,
            refreshToken: payload.refreshToken,
            expiresAt: nowProvider() + payload.expiresIn
        )
        saveToken(token, userID: payload.userID)
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
