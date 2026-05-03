import Foundation

protocol CommunityFeedServing: Sendable {
    func loadFeed() async throws -> CommunityFeedPayload
}

/// 运营可替换包内 `community_feed.json` 或通过 OTA 更新资源；列表结构与远端一致。
final class BundledCommunityFeedRepository: CommunityFeedServing, @unchecked Sendable {
    func loadFeed() async throws -> CommunityFeedPayload {
        try await Task.detached {
            guard let url = Bundle.main.url(forResource: "community_feed", withExtension: "json") else {
                return CommunityFeedPayload(official: [], user: [], source: .bundled)
            }
            let data = try Data(contentsOf: url)
            let file = try JSONDecoder().decode(CommunityFeedFile.self, from: data)
            return CommunityFeedPayload(official: file.official, user: file.user, source: .bundled)
        }.value
    }
}

/// GET `{baseURL}/community/feed`，响应体与 `community_feed.json` 相同。
final class HTTPCommunityFeedRepository: CommunityFeedServing, @unchecked Sendable {
    private let baseURL: URL
    private let timeout: TimeInterval

    init(baseURL: URL, timeout: TimeInterval = 8) {
        self.baseURL = baseURL
        self.timeout = timeout
    }

    func loadFeed() async throws -> CommunityFeedPayload {
        let url = baseURL.appendingPathComponent("community").appendingPathComponent("feed")
        var request = URLRequest(url: url)
        request.timeoutInterval = timeout
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        let file = try JSONDecoder().decode(CommunityFeedFile.self, from: data)
        return CommunityFeedPayload(official: file.official, user: file.user, source: .remote)
    }
}

/// 优先远端，失败则回退包内资源。
struct CompositeCommunityFeedRepository: CommunityFeedServing {
    private let remote: HTTPCommunityFeedRepository?
    private let bundled: BundledCommunityFeedRepository

    init(remote: HTTPCommunityFeedRepository?, bundled: BundledCommunityFeedRepository = BundledCommunityFeedRepository()) {
        self.remote = remote
        self.bundled = bundled
    }

    func loadFeed() async throws -> CommunityFeedPayload {
        if let remote {
            do {
                return try await remote.loadFeed()
            } catch {
                // fall through to bundled
            }
        }
        return try await bundled.loadFeed()
    }
}
