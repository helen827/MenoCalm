import Foundation

protocol RemoteJournalAPIClientProtocol {
    func fetchEntries(userID: String) throws -> [JournalEntryDTO]
    func uploadEntries(_ entries: [JournalEntryDTO], userID: String) throws
}

enum RemoteAPIError: Error, Equatable {
    case injectedFailure
    case endpointNotConfigured
    case invalidResponse
    case badRequest
    case unauthorized
    case forbidden
    case notFound
    case conflict
    case rateLimited
    case timeout
    case server(statusCode: Int)
    case transport(String)
    case decode(String)
}

extension RemoteAPIError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case .injectedFailure:
            return "远端请求被注入失败（测试模式）。"
        case .endpointNotConfigured:
            return "后端地址未配置，请在设置中填写 Backend Base URL。"
        case .invalidResponse:
            return "服务响应无效，请稍后重试。"
        case .badRequest:
            return "请求参数错误。"
        case .unauthorized:
            return "鉴权失败，请重新登录。"
        case .forbidden:
            return "没有访问权限。"
        case .notFound:
            return "请求的资源不存在。"
        case .conflict:
            return "数据冲突，请刷新后重试。"
        case .rateLimited:
            return "请求过于频繁，请稍后再试。"
        case .timeout:
            return "请求超时，请检查网络连接。"
        case let .server(statusCode):
            return "服务异常（\(statusCode)）。"
        case let .transport(detail):
            return "网络请求失败：\(detail)"
        case let .decode(detail):
            return "响应解析失败：\(detail)"
        }
    }
}

final class InMemoryRemoteJournalAPIClient: RemoteJournalAPIClientProtocol {
    static let shared = InMemoryRemoteJournalAPIClient()

    private var storage: [String: [JournalEntryDTO]] = [:]
    private var failingUsers: Set<String> = []

    private init() {}

    func setFailing(_ shouldFail: Bool, for userID: String) {
        if shouldFail {
            failingUsers.insert(userID)
        } else {
            failingUsers.remove(userID)
        }
    }

    func fetchEntries(userID: String) throws -> [JournalEntryDTO] {
        if failingUsers.contains(userID) {
            throw RemoteAPIError.injectedFailure
        }
        return storage[userID] ?? []
    }

    func uploadEntries(_ entries: [JournalEntryDTO], userID: String) throws {
        if failingUsers.contains(userID) {
            throw RemoteAPIError.injectedFailure
        }
        storage[userID] = entries
    }
}

struct BackendEnvironment {
    let baseURL: URL?
    let timeout: TimeInterval
    let tokenProvider: AuthTokenProviderProtocol

    static let unconfigured = BackendEnvironment(
        baseURL: nil,
        timeout: 8,
        tokenProvider: AuthTokenManager()
    )
}

struct HTTPRemoteJournalAPIClient: RemoteJournalAPIClientProtocol {
    private let session: URLSession
    private let env: BackendEnvironment

    init(session: URLSession = .shared, env: BackendEnvironment) {
        self.session = session
        self.env = env
    }

    func fetchEntries(userID: String) throws -> [JournalEntryDTO] {
        let endpoint = try endpointURL(path: "/api/v1/journal/entries", userID: userID)
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "GET"
        addAuthHeader(to: &request, userID: userID)
        let data = try send(request, userID: userID)
        do {
            return try JSONDecoder().decode([JournalEntryDTO].self, from: data)
        } catch {
            throw RemoteAPIError.decode("decode failed: \(error.localizedDescription)")
        }
    }

    func uploadEntries(_ entries: [JournalEntryDTO], userID: String) throws {
        let endpoint = try endpointURL(path: "/api/v1/journal/entries", userID: userID)
        var request = URLRequest(url: endpoint, timeoutInterval: env.timeout)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(to: &request, userID: userID)
        request.httpBody = try JSONEncoder().encode(entries)
        _ = try send(request, userID: userID)
    }

    private func endpointURL(path: String, userID: String) throws -> URL {
        guard let baseURL = env.baseURL else {
            throw RemoteAPIError.endpointNotConfigured
        }
        guard var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false) else {
            throw RemoteAPIError.invalidResponse
        }
        components.queryItems = [URLQueryItem(name: "userId", value: userID)]
        guard let url = components.url else {
            throw RemoteAPIError.invalidResponse
        }
        return url
    }

    private func addAuthHeader(to request: inout URLRequest, userID: String) {
        if let token = env.tokenProvider.validAccessToken(userID: userID), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
    }

    private func send(_ request: URLRequest, userID: String) throws -> Data {
        do {
            return try sendOnce(request)
        } catch RemoteAPIError.unauthorized {
            guard let refreshed = env.tokenProvider.refreshAccessToken(userID: userID), !refreshed.isEmpty else {
                env.tokenProvider.invalidate(userID: userID)
                throw RemoteAPIError.unauthorized
            }
            var retried = request
            retried.setValue("Bearer \(refreshed)", forHTTPHeaderField: "Authorization")
            do {
                return try sendOnce(retried)
            } catch RemoteAPIError.unauthorized {
                env.tokenProvider.invalidate(userID: userID)
                throw RemoteAPIError.unauthorized
            }
        }
    }

    private func sendOnce(_ request: URLRequest) throws -> Data {
        let semaphore = DispatchSemaphore(value: 0)
        var result: Result<Data, Error> = .failure(RemoteAPIError.invalidResponse)

        session.dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }

            if let error = error as NSError? {
                if error.code == NSURLErrorTimedOut {
                    result = .failure(RemoteAPIError.timeout)
                } else {
                    result = .failure(RemoteAPIError.transport(error.localizedDescription))
                }
                return
            }

            guard let http = response as? HTTPURLResponse else {
                result = .failure(RemoteAPIError.invalidResponse)
                return
            }

            if let mappedError = Self.mapStatusCode(http.statusCode) {
                result = .failure(mappedError)
                return
            }

            result = .success(data ?? Data())
        }.resume()

        _ = semaphore.wait(timeout: .now() + env.timeout + 1)
        return try result.get()
    }

    static func mapStatusCode(_ statusCode: Int) -> RemoteAPIError? {
        switch statusCode {
        case 200..<300: return nil
        case 400: return .badRequest
        case 401: return .unauthorized
        case 403: return .forbidden
        case 404: return .notFound
        case 409: return .conflict
        case 429: return .rateLimited
        case 500...599: return .server(statusCode: statusCode)
        default: return .server(statusCode: statusCode)
        }
    }
}
