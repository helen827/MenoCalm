import Foundation

struct RemoteAssistantReply {
    var text: String
    var source: String
    var degradedReason: String?
}

protocol RemoteConversationAPIClientProtocol {
    func generateReply(userID: String, conversationID: String, userText: String) throws -> RemoteAssistantReply
}

private struct ConversationMessageRequestDTO: Codable {
    var conversationId: String
    var role: String
    var content: String
}

private struct ConversationInsightDTO: Codable {
    var conversationId: String?
    var summary: String?
    var riskLevel: String?
    var symptomTags: [String]?
    var suggestions: [String]?
    var updatedAtMs: TimeInterval?
    var source: String?
    var degradedReason: String?
}

private struct ApiErrorResponseDTO: Codable {
    var code: String?
    var message: String?
    var requestId: String?
    var path: String?
    var timestamp: TimeInterval?
}

private struct InsightEnvelopeDTO: Codable {
    var data: ConversationInsightDTO?
    var result: ConversationInsightDTO?
    var insight: ConversationInsightDTO?
}

struct HTTPRemoteConversationAPIClient: RemoteConversationAPIClientProtocol {
    private let session: URLSession
    private let env: BackendEnvironment

    init(session: URLSession = .shared, env: BackendEnvironment) {
        self.session = session
        self.env = env
    }

    func generateReply(userID: String, conversationID: String, userText: String) throws -> RemoteAssistantReply {
        let appendURL = try endpointURL(path: "/api/v1/conversations/messages", userID: userID)
        let appendBody = ConversationMessageRequestDTO(
            conversationId: conversationID,
            role: "user",
            content: userText
        )

        var appendReq = URLRequest(url: appendURL, timeoutInterval: env.timeout)
        appendReq.httpMethod = "POST"
        appendReq.setValue("application/json", forHTTPHeaderField: "Content-Type")
        appendReq.httpBody = try JSONEncoder().encode(appendBody)
        addAuthHeader(to: &appendReq, userID: userID)
        _ = try send(appendReq, userID: userID)

        let analyzeURL = try endpointURL(path: "/api/v1/conversations/insight/\(conversationID)/analyze", userID: userID)
        var analyzeReq = URLRequest(url: analyzeURL, timeoutInterval: env.timeout)
        analyzeReq.httpMethod = "POST"
        addAuthHeader(to: &analyzeReq, userID: userID)
        let data = try send(analyzeReq, userID: userID)

        let insight = try decodeInsight(data)
        let source = (insight.source ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        return RemoteAssistantReply(
            text: formatReply(insight),
            source: source.isEmpty ? "remote" : source,
            degradedReason: insight.degradedReason
        )
    }

    private func formatReply(_ insight: ConversationInsightDTO) -> String {
        let summary = (insight.summary ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let risk = (insight.riskLevel ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let tags = (insight.symptomTags ?? [])
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .prefix(4)
        let suggestions = (insight.suggestions ?? [])
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .prefix(3)

        let riskHint: String
        switch risk {
        case "high":
            riskHint = "当前风险提示：偏高，建议尽快线下评估。"
        case "medium":
            riskHint = "当前风险提示：中等，建议持续观察并尽早干预。"
        default:
            riskHint = "当前风险提示：偏低，可先进行自我管理并持续观察。"
        }
        let tagsLine = tags.isEmpty ? "" : "\n我重点关注到：\(tags.joined(separator: "、"))。"

        if !summary.isEmpty, !suggestions.isEmpty {
            let lines = suggestions.enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")
            return decorateIfDegraded("\(summary)\(tagsLine)\n\(riskHint)\n\n你可以先这样做：\n\(lines)", insight: insight)
        }
        if !summary.isEmpty {
            return decorateIfDegraded("\(summary)\(tagsLine)\n\(riskHint)", insight: insight)
        }
        if !suggestions.isEmpty {
            return decorateIfDegraded("\(riskHint)\n\n你可以先这样做：\n" + suggestions.enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n"), insight: insight)
        }
        return decorateIfDegraded("我已经理解你的情况。你可以继续补充症状出现时间、频率和触发因素，我会给你更具体的下一步建议。", insight: insight)
    }

    private func decorateIfDegraded(_ text: String, insight: ConversationInsightDTO) -> String {
        guard let source = insight.source?.trimmingCharacters(in: .whitespacesAndNewlines), !source.isEmpty else {
            return text
        }
        if source.lowercased() == "fallback" {
#if DEBUG
            let reason = (insight.degradedReason ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if reason.isEmpty {
                return text + "\n\n（系统提示：远端模型暂不可用，已使用降级策略）"
            }
            return text + "\n\n（系统提示：远端模型暂不可用，已使用降级策略：\(reason)）"
#else
            return text
#endif
        }
        return text
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
            return try sendOnce(retried)
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
            if !(200..<300).contains(http.statusCode) {
                let detail = decodeApiErrorDetail(data)
                if http.statusCode == 401 {
                    result = .failure(RemoteAPIError.unauthorized)
                    return
                }
                if http.statusCode == 403 {
                    result = .failure(RemoteAPIError.forbidden)
                    return
                }
                if http.statusCode == 404 {
                    result = .failure(RemoteAPIError.notFound)
                    return
                }
                if http.statusCode == 409 {
                    result = .failure(RemoteAPIError.conflict)
                    return
                }
                if http.statusCode == 429 {
                    result = .failure(RemoteAPIError.rateLimited)
                    return
                }
                if http.statusCode == 400 {
                    result = .failure(RemoteAPIError.badRequest)
                    return
                }
                if http.statusCode >= 500, !detail.isEmpty {
                    result = .failure(RemoteAPIError.transport("服务异常(\(http.statusCode))：\(detail)"))
                    return
                }
                result = .failure(RemoteAPIError.server(statusCode: http.statusCode))
                return
            }
            result = .success(data ?? Data())
        }.resume()

        _ = semaphore.wait(timeout: .now() + env.timeout + 1)
        return try result.get()
    }

    private func decodeApiErrorDetail(_ data: Data?) -> String {
        guard let data, !data.isEmpty else { return "" }
        if let apiError = try? JSONDecoder().decode(ApiErrorResponseDTO.self, from: data) {
            let message = (apiError.message ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let code = (apiError.code ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let rid = (apiError.requestId ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            var parts: [String] = []
            if !message.isEmpty { parts.append(message) }
            if !code.isEmpty { parts.append("code=\(code)") }
            if !rid.isEmpty { parts.append("rid=\(rid)") }
            return parts.joined(separator: " ")
        }
        if let text = String(data: data, encoding: .utf8) {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.count > 200 {
                return String(trimmed.prefix(200))
            }
            return trimmed
        }
        return ""
    }

    private func decodeInsight(_ data: Data) throws -> ConversationInsightDTO {
        do {
            return try JSONDecoder().decode(ConversationInsightDTO.self, from: data)
        } catch {
            if let wrapped = try? JSONDecoder().decode(InsightEnvelopeDTO.self, from: data) {
                if let v = wrapped.data { return v }
                if let v = wrapped.result { return v }
                if let v = wrapped.insight { return v }
            }
            let detail = decodeApiErrorDetail(data)
            if !detail.isEmpty {
                throw RemoteAPIError.decode("conversation insight decode failed: \(detail)")
            }
            if let raw = String(data: data, encoding: .utf8) {
                let snippet = raw.trimmingCharacters(in: .whitespacesAndNewlines)
                if looksLikeCaptivePortalHTML(snippet) {
                    throw RemoteAPIError.transport("检测到网络门户重定向（疑似 Wi-Fi 登录页），请切换网络或完成门户认证")
                }
                throw RemoteAPIError.decode("conversation insight decode failed: \(String(snippet.prefix(180)))")
            }
            throw RemoteAPIError.decode("conversation insight decode failed: \(error.localizedDescription)")
        }
    }

    private func looksLikeCaptivePortalHTML(_ text: String) -> Bool {
        let lower = text.lowercased()
        if lower.contains("<html"), lower.contains("meta http-equiv='refresh'") {
            return true
        }
        if lower.contains("cmd=redirect") || lower.contains("arubalp") || lower.contains("captive") {
            return true
        }
        return false
    }
}
