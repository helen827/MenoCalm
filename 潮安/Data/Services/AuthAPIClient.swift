import Foundation

protocol AuthAPIClientProtocol {
    func sendPhoneCode(_ phone: String) throws
    func verifyPhoneCode(_ phone: String, code: String) throws -> AuthTokenPayload
    func loginWithPhone(_ phone: String) throws -> AuthTokenPayload
    func loginWithWeChatCode(_ code: String) throws -> AuthTokenPayload
    func refreshToken(_ refreshToken: String, userID: String) throws -> AuthTokenPayload
}

struct AuthTokenPayload: Codable {
    var accessToken: String
    var refreshToken: String
    var expiresIn: TimeInterval
    var userID: String
}

enum AuthAPIError: Error, Equatable {
    case endpointNotConfigured
    case invalidPhone
    case invalidResponse
    case unauthorized
    case forbidden
    case timeout
    case server(statusCode: Int)
    case transport(String)
    case decode(String)
}

final class InMemoryAuthAPIClient: AuthAPIClientProtocol {
    func sendPhoneCode(_ phone: String) throws {
        let digits = phone.filter(\.isNumber)
        guard digits.count == 11 else { throw AuthAPIError.invalidPhone }
    }

    func verifyPhoneCode(_ phone: String, code: String) throws -> AuthTokenPayload {
        let digits = phone.filter(\.isNumber)
        guard digits.count == 11 else { throw AuthAPIError.invalidPhone }
        guard code.count >= 4 else { throw AuthAPIError.unauthorized }
        return AuthTokenPayload(
            accessToken: "acc-\(digits)",
            refreshToken: "ref-\(digits)",
            expiresIn: 1800,
            userID: "phone_\(digits)"
        )
    }

    func loginWithPhone(_ phone: String) throws -> AuthTokenPayload {
        let digits = phone.filter(\.isNumber)
        guard !digits.isEmpty else { throw AuthAPIError.invalidPhone }
        return AuthTokenPayload(
            accessToken: "acc-\(digits)",
            refreshToken: "ref-\(digits)",
            expiresIn: 1800,
            userID: "phone_\(digits)"
        )
    }

    func refreshToken(_ refreshToken: String, userID: String) throws -> AuthTokenPayload {
        guard !refreshToken.isEmpty else { throw AuthAPIError.unauthorized }
        return AuthTokenPayload(
            accessToken: "acc-\(userID)-\(Int(Date().timeIntervalSince1970))",
            refreshToken: refreshToken,
            expiresIn: 3600,
            userID: userID
        )
    }

    func loginWithWeChatCode(_ code: String) throws -> AuthTokenPayload {
        let normalized = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { throw AuthAPIError.unauthorized }
        let uid = "wx_\(String(normalized.prefix(12)))"
        return AuthTokenPayload(
            accessToken: "acc-\(uid)",
            refreshToken: "ref-\(uid)",
            expiresIn: 1800,
            userID: uid
        )
    }
}

struct AuthBackendEnvironment {
    let baseURL: URL?
    let timeout: TimeInterval

    static let unconfigured = AuthBackendEnvironment(baseURL: nil, timeout: 8)
}

struct HTTPAuthAPIClient: AuthAPIClientProtocol {
    private let session: URLSession
    private let env: AuthBackendEnvironment

    init(session: URLSession = .shared, env: AuthBackendEnvironment) {
        self.session = session
        self.env = env
    }

    func loginWithPhone(_ phone: String) throws -> AuthTokenPayload {
        guard let endpoint = endpoint(path: "/api/v1/auth/phone/login") else {
            throw AuthAPIError.endpointNotConfigured
        }
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["phone": phone.filter(\.isNumber)])
        let data = try send(request)
        do {
            return try JSONDecoder().decode(AuthTokenPayload.self, from: data)
        } catch {
            throw AuthAPIError.decode("decode failed: \(error.localizedDescription)")
        }
    }

    func sendPhoneCode(_ phone: String) throws {
        guard let endpoint = endpoint(path: "/api/v1/auth/phone/code/send") else {
            throw AuthAPIError.endpointNotConfigured
        }
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["phone": phone.filter(\.isNumber)])
        _ = try send(request)
    }

    func verifyPhoneCode(_ phone: String, code: String) throws -> AuthTokenPayload {
        guard let endpoint = endpoint(path: "/api/v1/auth/phone/code/verify") else {
            throw AuthAPIError.endpointNotConfigured
        }
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["phone": phone.filter(\.isNumber), "code": code])
        let data = try send(request)
        do {
            return try JSONDecoder().decode(AuthTokenPayload.self, from: data)
        } catch {
            throw AuthAPIError.decode("decode failed: \(error.localizedDescription)")
        }
    }

    func refreshToken(_ refreshToken: String, userID: String) throws -> AuthTokenPayload {
        guard let endpoint = endpoint(path: "/api/v1/auth/refresh") else {
            throw AuthAPIError.endpointNotConfigured
        }
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["refreshToken": refreshToken, "userId": userID])
        let data = try send(request)
        do {
            return try JSONDecoder().decode(AuthTokenPayload.self, from: data)
        } catch {
            throw AuthAPIError.decode("decode failed: \(error.localizedDescription)")
        }
    }

    func loginWithWeChatCode(_ code: String) throws -> AuthTokenPayload {
        guard let endpoint = endpoint(path: "/api/v1/auth/wechat/login") else {
            throw AuthAPIError.endpointNotConfigured
        }
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["code": code])
        let data = try send(request)
        do {
            return try JSONDecoder().decode(AuthTokenPayload.self, from: data)
        } catch {
            throw AuthAPIError.decode("decode failed: \(error.localizedDescription)")
        }
    }

    private func endpoint(path: String) -> URL? {
        env.baseURL?.appendingPathComponent(path)
    }

    private func send(_ request: URLRequest) throws -> Data {
        let semaphore = DispatchSemaphore(value: 0)
        var result: Result<Data, Error> = .failure(AuthAPIError.invalidResponse)

        session.dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }

            if let error = error as NSError? {
                if error.code == NSURLErrorTimedOut {
                    result = .failure(AuthAPIError.timeout)
                } else {
                    result = .failure(AuthAPIError.transport(error.localizedDescription))
                }
                return
            }

            guard let http = response as? HTTPURLResponse else {
                result = .failure(AuthAPIError.invalidResponse)
                return
            }

            if let mapped = Self.mapStatusCode(http.statusCode) {
                result = .failure(mapped)
                return
            }

            result = .success(data ?? Data())
        }.resume()

        _ = semaphore.wait(timeout: .now() + env.timeout + 1)
        return try result.get()
    }

    static func mapStatusCode(_ code: Int) -> AuthAPIError? {
        switch code {
        case 200..<300: return nil
        case 401: return .unauthorized
        case 403: return .forbidden
        case 500...599: return .server(statusCode: code)
        default: return .server(statusCode: code)
        }
    }
}
