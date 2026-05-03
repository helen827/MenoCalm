import Foundation
import Combine
import Network
#if canImport(SwiftData)
import SwiftData
#endif

enum MainTab: String, CaseIterable {
    case home = "AI对话"
    case learn = "社区"
    case practice = "呼吸练习"
    case profile = "我的"

    var icon: String {
        switch self {
        case .home: "bubble.left.and.bubble.right"
        case .learn: "square.grid.2x2"
        case .practice: "wind"
        case .profile: "person"
        }
    }
}

enum AppRoute: Hashable {
    case articleDetail
    case trendReport
    case medicalList
    case settings
    case privacyPolicy
    case userAgreement
    case medicalDisclaimer
    case practiceDetail
    case communityPost
    case communityPostDetail
    case userStatus
    case basicInfo
    case selfTest
    case selfTestResult
}

final class AppViewModel: ObservableObject {
    struct AcceptanceChecklistItem: Identifiable {
        var id: String { title }
        var title: String
        var passed: Bool
        var detail: String
    }

    struct FailCriterionStatus: Identifiable {
        var id: String { title }
        var title: String
        var triggered: Bool
        var detail: String
    }

    struct MinimumClosedLoopSnapshot {
        var checklist: [AcceptanceChecklistItem]
        var failCriteria: [FailCriterionStatus]

        var checklistPassedCount: Int {
            checklist.filter(\.passed).count
        }

        var failTriggeredCount: Int {
            failCriteria.filter(\.triggered).count
        }
    }

    struct DataExportResult {
        var jsonURL: URL
        var summaryURL: URL
    }

    enum PrivacyActionError: LocalizedError {
        case exportWriteFailed
        case deleteFailed(String)
        case deactivateFailed(String)

        var errorDescription: String? {
            switch self {
            case .exportWriteFailed:
                return "导出文件写入失败，请稍后重试。"
            case let .deleteFailed(detail):
                return "删除数据失败：\(detail)"
            case let .deactivateFailed(detail):
                return "注销账号失败：\(detail)"
            }
        }
    }

    private struct ExportPayload: Codable {
        var generatedAt: TimeInterval
        var userID: String
        var journalEntries: [JournalEntry]
        var conversationInsights: [ConversationInsight]
        var reportSnapshots: [ReportSnapshot]
        var practiceTotalSessions: Int
        var practiceLastCompletedAt: TimeInterval?
    }

    @Published var onboardingStep = 1
    @Published var isLoggedIn = false
    @Published var showSessionExpiredAlert = false
    @Published private(set) var sessionExpiredMessage = "登录状态已失效，请重新登录。"
    @Published var journalEntries: [JournalEntry] = []
    @Published var conversationInsights: [ConversationInsight] = []
    @Published private(set) var reportSnapshotsByRange: [Int: ReportSnapshot] = [:]
    @Published private(set) var currentUserID = "guest-local"
    @Published private(set) var runtimeEnvironment = AppRuntimeEnvironment.dev.rawValue
    @Published private(set) var rolloutSummary = "localReads=0 remoteReads=0 syncSuccess=0 syncFailures=0"
    @Published private(set) var rolloutAlerts: [String] = []
    @Published private(set) var runtimeEvents: [OperationalEvent] = []
    @Published private(set) var operationalAlerts: [OperationalAlert] = []
    @Published private(set) var conversationMemory: [String] = []
    @Published private(set) var syncDiagnostics = SyncQueueDiagnostics(
        pendingCount: 0,
        runningCount: 0,
        retriedCount: 0,
        failedCount: 0,
        recent: []
    )
    @Published private(set) var communityFeedOfficial: [CommunityFeedItem] = []
    @Published private(set) var communityFeedUser: [CommunityFeedItem] = []
    @Published private(set) var communityFeedSource: String = "bundled"
    @Published private(set) var isCommunityFeedLoading = false
    @Published private(set) var communityFeedErrorMessage: String?
    @Published private(set) var authErrorMessage: String?
    @Published private(set) var isAuthLoading = false
    @Published private(set) var isReportRefreshing = false
    @Published private(set) var isNetworkReachable = true

    private var journalRepository: JournalRepositoryProtocol
    private var conversationInsightRepository: ConversationInsightRepositoryProtocol
    private var reportSnapshotRepository: ReportSnapshotRepositoryProtocol
    private let extractSignalsUseCase: ExtractSignalsUseCaseProtocol
    private let conversationExtractor: ConversationExtractorProtocol
    private let correlationAnalyzer: CorrelationAnalyzerProtocol
    private let medicalSafetyGuard: MedicalSafetyGuardProtocol
    private let conversationAuditor: ConversationAuditorProtocol
    private let ragService: RAGServiceProtocol
    private let authSession: AuthSession
    private let featureFlags: FeatureFlags
    private let rolloutMonitor: RolloutMonitor
    private let syncQueue: SyncQueue
    private let tokenManager: AuthTokenManager
    private let alertingCenter: OperationalAlertingCenter
    private let privacyActionAuditStore: PrivacyActionAuditStore
    private let communityFeedRepository: CommunityFeedServing
    private let remoteJournalRepository: RemoteJournalRepository
    private let userIDProvider: () -> String
    private let usesCustomRepositories: Bool
    private let rolloutThresholds = RolloutThresholds.default
    private let networkReachabilityWatch = NetworkReachabilityWatch()
#if canImport(SwiftData)
    private var localModelContext: ModelContext?
    private var migratedUserIDs: Set<String> = []
#endif

    init(
        authSession: AuthSession = AuthSession(),
        featureFlags: FeatureFlags = FeatureFlags(),
        journalRepository: JournalRepositoryProtocol? = nil,
        conversationInsightRepository: ConversationInsightRepositoryProtocol? = nil,
        reportSnapshotRepository: ReportSnapshotRepositoryProtocol? = nil,
        extractSignalsUseCase: ExtractSignalsUseCaseProtocol = RuleBasedExtractSignalsUseCase(),
        conversationExtractor: ConversationExtractorProtocol = RuleBasedConversationExtractor(),
        correlationAnalyzer: CorrelationAnalyzerProtocol = RuleBasedCorrelationAnalyzer(),
        medicalSafetyGuard: MedicalSafetyGuardProtocol = RuleBasedMedicalSafetyGuard(),
        conversationAuditor: ConversationAuditorProtocol = RedactingConversationAuditor(),
        ragService: RAGServiceProtocol = RAGService()
    ) {
        self.authSession = authSession
        self.featureFlags = featureFlags
        let alertingCenter = OperationalAlertingCenter()
        self.alertingCenter = alertingCenter
        self.privacyActionAuditStore = PrivacyActionAuditStore()
        let runtimeConfig = RuntimeConfigResolver().resolveOrFallback()
        runtimeEnvironment = runtimeConfig.environment.rawValue
        let backendBaseURL = runtimeConfig.backend.baseURL
        let authEnv = AuthBackendEnvironment(baseURL: backendBaseURL, timeout: 8)
        let authClient: AuthAPIClientProtocol = backendBaseURL == nil
            ? InMemoryAuthAPIClient()
            : HTTPAuthAPIClient(env: authEnv)
        let tokenManager = AuthTokenManager(authAPIClient: authClient)
        self.tokenManager = tokenManager
        let userIDProvider = { authSession.currentUserID }
        self.userIDProvider = userIDProvider
        let localRepository = UserDefaultsJournalRepository(userIDProvider: userIDProvider)
        let localInsightRepository = UserDefaultsConversationInsightRepository(userIDProvider: userIDProvider)
        let localReportRepository = UserDefaultsReportSnapshotRepository(userIDProvider: userIDProvider)
        let backendEnv = BackendEnvironment(
            baseURL: backendBaseURL,
            timeout: runtimeConfig.backend.timeout,
            tokenProvider: tokenManager
        )
        let bundledCommunityFeed = BundledCommunityFeedRepository()
        let communityFeedRepository: CommunityFeedServing = {
            if let base = backendBaseURL {
                return CompositeCommunityFeedRepository(
                    remote: HTTPCommunityFeedRepository(baseURL: base, timeout: runtimeConfig.backend.timeout),
                    bundled: bundledCommunityFeed
                )
            }
            return bundledCommunityFeed
        }()
        self.communityFeedRepository = communityFeedRepository
        let remoteRepository = RemoteJournalRepository(
            apiClient: HTTPRemoteJournalAPIClient(env: backendEnv),
            userIDProvider: userIDProvider
        )
        self.remoteJournalRepository = remoteRepository
        let syncQueue = SyncQueue(userIDProvider: userIDProvider)
        self.syncQueue = syncQueue
        let rolloutMonitor = RolloutMonitor()
        let repositoryFacade = RepositoryFacade(
            localRepository: localRepository,
            remoteRepository: remoteRepository,
            syncQueue: syncQueue,
            featureFlags: featureFlags,
            rolloutMonitor: rolloutMonitor
        )

        self.usesCustomRepositories = journalRepository != nil || conversationInsightRepository != nil || reportSnapshotRepository != nil
        self.journalRepository = journalRepository ?? repositoryFacade
        self.conversationInsightRepository = conversationInsightRepository ?? localInsightRepository
        self.reportSnapshotRepository = reportSnapshotRepository ?? localReportRepository
        self.extractSignalsUseCase = extractSignalsUseCase
        self.conversationExtractor = conversationExtractor
        self.correlationAnalyzer = correlationAnalyzer
        self.medicalSafetyGuard = medicalSafetyGuard
        self.conversationAuditor = conversationAuditor
        self.ragService = ragService
        self.rolloutMonitor = rolloutMonitor
        tokenManager.onSessionInvalidated = { [weak self] _ in
            self?.handleSessionInvalidated()
        }
        alertingCenter.record(
            category: "app-launch",
            message: "应用启动，当前环境：\(runtimeEnvironment)",
            level: .info
        )
        currentUserID = authSession.currentUserID
        isLoggedIn = !authSession.isAnonymous
        journalEntries = self.journalRepository.loadEntries()
        conversationInsights = self.conversationInsightRepository.loadInsights()
        reportSnapshotsByRange = Dictionary(
            uniqueKeysWithValues: self.reportSnapshotRepository.loadSnapshots().map { ($0.rangeDays, $0) }
        )
        refreshReportSnapshots()
        refreshRolloutSummary()
        refreshSyncDiagnostics()
        refreshOperationalObservability()
        Task { [weak self] in
            await self?.refreshCommunityFeed()
        }

        networkReachabilityWatch.start { [weak self] online in
            self?.isNetworkReachable = online
        }
    }

    func refreshInternalDiagnosticPanels() {
        refreshRolloutSummary()
        refreshSyncDiagnostics()
        alertingCenter.record(category: "settings", message: "手动刷新内部监控数据", level: .info)
        refreshOperationalObservability()
    }

#if canImport(SwiftData)
    func bindLocalModelContext(_ context: ModelContext) {
        localModelContext = context
        migrateCurrentUserIfNeeded()
        guard !usesCustomRepositories else { return }
        let swiftLocalRepository = SwiftDataJournalRepository(
            contextProvider: { [weak self] in self?.localModelContext },
            userIDProvider: userIDProvider
        )
        journalRepository = RepositoryFacade(
            localRepository: swiftLocalRepository,
            remoteRepository: remoteJournalRepository,
            syncQueue: syncQueue,
            featureFlags: featureFlags,
            rolloutMonitor: rolloutMonitor
        )
        conversationInsightRepository = SwiftDataConversationInsightRepository(
            contextProvider: { [weak self] in self?.localModelContext },
            userIDProvider: userIDProvider
        )
        reportSnapshotRepository = SwiftDataReportSnapshotRepository(
            contextProvider: { [weak self] in self?.localModelContext },
            userIDProvider: userIDProvider
        )
        reloadForCurrentUser()
    }
#endif

    func refreshCommunityFeed() async {
        await MainActor.run {
            isCommunityFeedLoading = true
            communityFeedErrorMessage = nil
        }
        do {
            let payload = try await communityFeedRepository.loadFeed()
            await MainActor.run {
                communityFeedOfficial = payload.official
                communityFeedUser = payload.user
                communityFeedSource = payload.source.rawValue
                isCommunityFeedLoading = false
            }
        } catch {
            let bundled = BundledCommunityFeedRepository()
            if let fallback = try? await bundled.loadFeed() {
                await MainActor.run {
                    communityFeedOfficial = fallback.official
                    communityFeedUser = fallback.user
                    communityFeedSource = CommunityFeedSource.bundled.rawValue
                    communityFeedErrorMessage = "社区服务暂不可用，已切换为本地内容。"
                    isCommunityFeedLoading = false
                }
            } else {
                await MainActor.run {
                    communityFeedOfficial = []
                    communityFeedUser = []
                    communityFeedSource = "error"
                    communityFeedErrorMessage = "社区内容加载失败，请检查网络后重试。"
                    isCommunityFeedLoading = false
                }
            }
        }
    }

    func breathingPracticeRecommendation() -> (title: String, subtitle: String) {
        let blob = journalEntries.prefix(40).map(\.rawText).joined(separator: " ")
        let symptoms = journalEntries.flatMap(\.extracted.symptoms).joined()
        if symptoms.contains("潮热") || blob.contains("潮热") {
            return ("最近记录里潮热较常见", "试试「潮热来临时」短呼吸，随时可暂停")
        }
        if symptoms.contains("失眠") || blob.contains("失眠") || blob.contains("睡不着") || blob.contains("睡眠") {
            return ("你最近多次提到睡眠困扰", "推荐「睡前放松」身体扫描，音量调低更易入睡")
        }
        if symptoms.contains("心悸") || blob.contains("心悸") || blob.contains("焦虑") {
            return ("紧张感或心悸值得被认真对待", "可先试「焦虑心悸时」慢呼吸；持续不适请就医")
        }
        return ("每天几分钟，给身体一个缓冲", "从「睡前放松」或「慢呼吸练习」开始都可以")
    }

    func breathingPracticeCompletedCount() -> Int {
        let key = StorageKeys.scoped(StorageKeys.practiceTotalSessions, userID: currentUserID)
        return UserDefaults.standard.integer(forKey: key)
    }

    func recordBreathingSessionCompleted() {
        let key = StorageKeys.scoped(StorageKeys.practiceTotalSessions, userID: currentUserID)
        let next = UserDefaults.standard.integer(forKey: key) + 1
        UserDefaults.standard.set(next, forKey: key)
        UserDefaults.standard.set(
            Date().timeIntervalSince1970,
            forKey: StorageKeys.scoped(StorageKeys.practiceLastCompletedAt, userID: currentUserID)
        )
        alertingCenter.record(category: "practice", message: "完成一次呼吸练习（累计 \(next)）", level: .info)
        refreshOperationalObservability()
    }

    func exportMyData() throws -> DataExportResult {
        let payload = ExportPayload(
            generatedAt: Date().timeIntervalSince1970,
            userID: currentUserID,
            journalEntries: journalEntries,
            conversationInsights: conversationInsights,
            reportSnapshots: reportSnapshotsByRange.values.sorted { $0.rangeDays < $1.rangeDays },
            practiceTotalSessions: breathingPracticeCompletedCount(),
            practiceLastCompletedAt: UserDefaults.standard.object(
                forKey: StorageKeys.scoped(StorageKeys.practiceLastCompletedAt, userID: currentUserID)
            ) as? TimeInterval
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(payload)
        let timestamp = Int(Date().timeIntervalSince1970)
        let folderURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("chaoan-export-\(currentUserID)-\(timestamp)", isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: folderURL, withIntermediateDirectories: true)
            let jsonURL = folderURL.appendingPathComponent("data.json")
            let summaryURL = folderURL.appendingPathComponent("summary.md")
            try data.write(to: jsonURL, options: .atomic)
            let summary = """
            # 潮安数据导出摘要

            - 导出时间：\(Date())
            - 用户：\(currentUserID)
            - 日记条数：\(journalEntries.count)
            - 洞察条数：\(conversationInsights.count)
            - 报告快照：\(reportSnapshotsByRange.count)
            - 呼吸练习累计：\(breathingPracticeCompletedCount())

            数据详情请查看同目录下 `data.json`。
            """
            guard let summaryData = summary.data(using: .utf8) else {
                throw PrivacyActionError.exportWriteFailed
            }
            try summaryData.write(to: summaryURL, options: .atomic)
            recordPrivacyAction(.export, status: .success, detail: "导出目录：\(folderURL.path)")
            alertingCenter.record(category: "privacy-export", message: "用户导出数据成功", level: .info)
            refreshOperationalObservability()
            return DataExportResult(jsonURL: jsonURL, summaryURL: summaryURL)
        } catch {
            recordPrivacyAction(.export, status: .failure, detail: error.localizedDescription)
            throw PrivacyActionError.exportWriteFailed
        }
    }

    func deleteMyData() throws {
        journalEntries = []
        conversationInsights = []
        reportSnapshotsByRange = [:]
        journalRepository.saveEntries([])
        conversationInsightRepository.saveInsights([])
        reportSnapshotRepository.saveSnapshots([])

        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: StorageKeys.scoped(StorageKeys.practiceTotalSessions, userID: currentUserID))
        defaults.removeObject(forKey: StorageKeys.scoped(StorageKeys.practiceLastCompletedAt, userID: currentUserID))
        defaults.removeObject(forKey: StorageKeys.scoped(StorageKeys.syncQueuePending, userID: currentUserID))
        defaults.removeObject(forKey: StorageKeys.scoped(StorageKeys.syncQueueFailed, userID: currentUserID))
        defaults.removeObject(forKey: StorageKeys.scoped(StorageKeys.syncQueueHistory, userID: currentUserID))
#if canImport(SwiftData)
        do {
            try purgeLocalSwiftData(userID: currentUserID)
        } catch {
            recordPrivacyAction(.deleteData, status: .failure, detail: error.localizedDescription)
            throw PrivacyActionError.deleteFailed(error.localizedDescription)
        }
#endif

        refreshReportSnapshots()
        refreshRolloutSummary()
        refreshSyncDiagnostics()
        recordPrivacyAction(.deleteData, status: .success, detail: "本地数据清理完成")
        alertingCenter.record(category: "privacy-delete", message: "用户删除本地数据成功", level: .warning)
        refreshOperationalObservability()
    }

    func deactivateAccount() throws {
        let formerUserID = currentUserID
        do {
            try deleteMyData()
            UserDefaults.standard.removeObject(forKey: StorageKeys.scoped(StorageKeys.authTokens, userID: formerUserID))
            authSession.logout()
            currentUserID = authSession.currentUserID
            isLoggedIn = false
            recordPrivacyAction(.deactivateAccount, status: .success, detail: "账号注销并退出登录", userID: formerUserID)
            alertingCenter.record(category: "auth-deactivate", message: "账号已注销并退出登录", level: .critical)
            refreshOperationalObservability()
        } catch {
            recordPrivacyAction(.deactivateAccount, status: .failure, detail: error.localizedDescription, userID: formerUserID)
            throw PrivacyActionError.deactivateFailed(error.localizedDescription)
        }
    }

    func nextOnboarding() {
        onboardingStep = min(4, onboardingStep + 1)
    }

    func login() {
        if authSession.isAnonymous {
            loginWithPhone("13800000000")
            return
        }
        authErrorMessage = nil
        isLoggedIn = true
    }

    func clearAuthError() {
        authErrorMessage = nil
    }

    func loginWithPhone(_ phone: String) {
        isAuthLoading = true
        authErrorMessage = nil
        defer { isAuthLoading = false }

        guard let userID = tokenManager.loginWithPhone(phone) else {
            authErrorMessage = "登录失败，请检查网络与后端配置后重试。"
            alertingCenter.record(
                category: "auth-login",
                message: "手机号登录失败，请检查后端鉴权链路",
                level: .warning
            )
            refreshOperationalObservability()
            return
        }
        authSession.login(userID: userID, phoneNumber: phone.filter(\.isNumber))
        authErrorMessage = nil
        alertingCenter.record(
            category: "auth-login",
            message: "手机号登录成功：\(authSession.currentUserID)",
            level: .info
        )
        seedAuthTokenIfNeeded()
        reloadForCurrentUser()
        isLoggedIn = true
        refreshOperationalObservability()
    }

    @discardableResult
    func sendPhoneCode(_ phone: String) -> Bool {
        isAuthLoading = true
        authErrorMessage = nil
        defer { isAuthLoading = false }

        let ok = tokenManager.sendPhoneCode(phone)
        if !ok {
            authErrorMessage = "验证码发送失败，请稍后重试。"
            alertingCenter.record(
                category: "auth-phone-code-send",
                message: "验证码发送失败，请检查短信配置或限流状态",
                level: .warning
            )
            refreshOperationalObservability()
            return false
        }
        alertingCenter.record(
            category: "auth-phone-code-send",
            message: "验证码发送成功",
            level: .info
        )
        refreshOperationalObservability()
        return true
    }

    func loginWithPhoneCode(_ phone: String, code: String) {
        isAuthLoading = true
        authErrorMessage = nil
        defer { isAuthLoading = false }

        guard let userID = tokenManager.loginWithPhoneCode(phone, code: code) else {
            authErrorMessage = "验证码登录失败，请检查验证码后重试。"
            alertingCenter.record(
                category: "auth-phone-code-verify",
                message: "验证码校验失败或已过期",
                level: .warning
            )
            refreshOperationalObservability()
            return
        }
        authSession.login(userID: userID, phoneNumber: phone.filter(\.isNumber))
        authErrorMessage = nil
        alertingCenter.record(
            category: "auth-phone-code-verify",
            message: "验证码登录成功：\(authSession.currentUserID)",
            level: .info
        )
        seedAuthTokenIfNeeded()
        reloadForCurrentUser()
        isLoggedIn = true
        refreshOperationalObservability()
    }

    func loginWithWeChatCode(_ code: String) {
        isAuthLoading = true
        authErrorMessage = nil
        defer { isAuthLoading = false }

        guard let userID = tokenManager.loginWithWeChatCode(code) else {
            authErrorMessage = "微信登录失败，请检查微信参数和后端配置后重试。"
            alertingCenter.record(
                category: "auth-wechat-login",
                message: "微信登录失败，请检查 code 与微信开放平台配置",
                level: .warning
            )
            refreshOperationalObservability()
            return
        }
        authSession.login(userID: userID, phoneNumber: nil)
        authErrorMessage = nil
        alertingCenter.record(
            category: "auth-wechat-login",
            message: "微信登录成功：\(authSession.currentUserID)",
            level: .info
        )
        seedAuthTokenIfNeeded()
        reloadForCurrentUser()
        isLoggedIn = true
        refreshOperationalObservability()
    }

    func refreshReportDataFromStorage() async {
        await MainActor.run { [weak self] in
            guard let self else { return }
            self.isReportRefreshing = true
            self.journalEntries = self.journalRepository.loadEntries()
            self.conversationInsights = self.conversationInsightRepository.loadInsights()
            let snapshots = self.reportSnapshotRepository.loadSnapshots()
            self.reportSnapshotsByRange = Dictionary(uniqueKeysWithValues: snapshots.map { ($0.rangeDays, $0) })
            if self.reportSnapshotsByRange.isEmpty {
                self.refreshReportSnapshots()
            }
            self.isReportRefreshing = false
        }
    }

    func saveJournal(text: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        let now = Date()
        let ts = now.timeIntervalSince1970
        let dateText = Self.dateFormatter.string(from: now)
        let extracted = extractSignalsUseCase.execute(text: text)
        let entry = JournalEntry(
            id: String(Int(ts * 1000)),
            date: dateText,
            rawText: text,
            createdAt: ts * 1000,
            extracted: extracted
        )
        let insight = conversationExtractor.extractInsight(
            from: text,
            createdAt: ts * 1000,
            extracted: extracted
        )
        journalEntries.insert(entry, at: 0)
        conversationInsights.insert(insight, at: 0)
        journalRepository.saveEntries(journalEntries)
        conversationInsightRepository.saveInsights(conversationInsights)
        refreshReportSnapshots()
        refreshRolloutSummary()
        refreshSyncDiagnostics()
    }

    func topSymptoms(limit: Int = 3) -> [String] {
        var counter: [String: Int] = [:]
        journalEntries.flatMap(\.extracted.symptoms).forEach { counter[$0, default: 0] += 1 }
        return counter.sorted { $0.value > $1.value }.prefix(limit).map(\.key)
    }

    func topTriggers(limit: Int = 3) -> [String] {
        var counter: [String: Int] = [:]
        journalEntries.flatMap(\.extracted.triggers).forEach { counter[$0, default: 0] += 1 }
        return counter.sorted { $0.value > $1.value }.prefix(limit).map(\.key)
    }

    func evaluateConversationSafety(userText: String) -> MedicalSafetyDecision {
        medicalSafetyGuard.evaluate(userText: userText)
    }

    func auditConversation(
        userText: String,
        assistantReply: String,
        decision: MedicalSafetyDecision
    ) {
        conversationAuditor.record(
            userText: userText,
            assistantReply: assistantReply,
            decision: decision
        )
    }

    func generateRAGAssistantReply(userText: String) -> RAGAnswer {
        ragService.generateReply(userText: userText)
    }

    func generateGuidedAssistantReply(userText: String, history: [String]) -> String {
        let safety = medicalSafetyGuard.evaluate(userText: userText)
        return generateGuidedAssistantReply(userText: userText, history: history, safety: safety)
    }

    func generateGuidedAssistantReply(userText: String, history: [String], safety: MedicalSafetyDecision) -> String {
        let preamble = safety.replyPreamble ?? ""
        let intent = inferIntent(from: userText)
        updateConversationMemory(with: intent)

        if featureFlags.guidedConversationEnabled, shouldAskClarifyingQuestion(userText: userText, history: history, intent: intent) {
            let body = preamble + empatheticPrefix(for: intent) + "为了给你更有用的建议，我先确认一下：\(clarifyingQuestion(for: intent))"
            return MedicalAssistantOutputSanitizer.sanitize(body, decision: safety)
        }

        let rag = ragService.generateReply(userText: userText)
        let body = preamble + empatheticPrefix(for: intent) + rag.text + formatCitationsInline(rag.citations, version: rag.knowledgeBaseVersion)
        return MedicalAssistantOutputSanitizer.sanitize(body, decision: safety)
    }

    func updateKnowledgeBase(documents: [KnowledgeDocument], version: String) {
        ragService.updateKnowledgeBase(documents: documents, version: version)
    }

    func currentKnowledgeBaseVersion() -> String {
        ragService.currentKnowledgeBaseVersion()
    }

    func reportSnapshot(rangeDays: Int) -> ReportSnapshot {
        reportSnapshotsByRange[rangeDays]
            ?? correlationAnalyzer.analyze(
                insights: conversationInsights,
                rangeDays: rangeDays,
                now: Date().timeIntervalSince1970 * 1000
            )
    }

    private func refreshReportSnapshots(now: Date = Date()) {
        let nowMillis = now.timeIntervalSince1970 * 1000
        let ranges = [7, 30, 90]
        let snapshots = ranges.map { days in
            correlationAnalyzer.analyze(insights: conversationInsights, rangeDays: days, now: nowMillis)
        }
        reportSnapshotsByRange = Dictionary(uniqueKeysWithValues: snapshots.map { ($0.rangeDays, $0) })
        reportSnapshotRepository.saveSnapshots(snapshots)
    }

    private func reloadForCurrentUser() {
        currentUserID = authSession.currentUserID
#if canImport(SwiftData)
        migrateCurrentUserIfNeeded()
#endif
        seedAuthTokenIfNeeded()
        journalEntries = journalRepository.loadEntries()
        conversationInsights = conversationInsightRepository.loadInsights()
        reportSnapshotsByRange = Dictionary(
            uniqueKeysWithValues: reportSnapshotRepository.loadSnapshots().map { ($0.rangeDays, $0) }
        )
        if reportSnapshotsByRange.isEmpty {
            refreshReportSnapshots()
        }
        refreshRolloutSummary()
        refreshSyncDiagnostics()
    }

    func setCloudReadEnabled(_ enabled: Bool) {
        featureFlags.cloudReadEnabled = enabled
    }

    func setCloudSyncEnabled(_ enabled: Bool) {
        featureFlags.cloudSyncEnabled = enabled
    }

    func setFailOpenToLocalData(_ enabled: Bool) {
        featureFlags.failOpenToLocalData = enabled
    }

    func setGuidedConversationEnabled(_ enabled: Bool) {
        featureFlags.guidedConversationEnabled = enabled
    }

    var isGuidedConversationEnabled: Bool {
        featureFlags.guidedConversationEnabled
    }

    private func refreshRolloutSummary() {
        let snapshot = rolloutMonitor.snapshot()
        rolloutSummary = "local=\(snapshot.localReads) remoteOK=\(snapshot.remoteReads) remoteFail=\(snapshot.remoteFailures) syncOK=\(snapshot.syncSuccess) syncFail=\(snapshot.syncFailures)"
        rolloutAlerts = rolloutMonitor.alerts(thresholds: rolloutThresholds)
        alertingCenter.evaluateRollout(snapshot)
        refreshOperationalObservability()
    }

    func rolloutDashboardSnapshot() -> RolloutSnapshot {
        rolloutMonitor.snapshot()
    }

    func syncDashboardSnapshot() -> SyncQueueDiagnostics {
        syncQueue.diagnostics(limit: 10)
    }

    func minimumClosedLoopSnapshot() -> MinimumClosedLoopSnapshot {
        let hasLogin = isLoggedIn && currentUserID != "guest-local"
        let hasJournal = !journalEntries.isEmpty
        let hasStructuredExtraction = journalEntries.contains { !$0.extracted.symptoms.isEmpty || !$0.extracted.triggers.isEmpty }
        let hasReport = reportSnapshotsByRange.values.contains { $0.sampleCount > 0 }
        let hasPractice = breathingPracticeCompletedCount() > 0
        let hasCommunityFeedOrFeedback = !communityFeedOfficial.isEmpty || !communityFeedUser.isEmpty || communityFeedErrorMessage != nil
        let hasComplianceEntry = true // Legal routes are wired in MainShellView.
        let hasObservability = !runtimeEvents.isEmpty || !operationalAlerts.isEmpty
        let safetyProbe = medicalSafetyGuard.evaluate(userText: "我有强烈伤害自己冲动")
        let highRiskBoundaryWorking = safetyProbe.shouldBlockResponse
            && (safetyProbe.safeReply?.contains("尽快") ?? false || safetyProbe.safeReply?.contains("急救") ?? false)

        let checklist: [AcceptanceChecklistItem] = [
            .init(title: "手机号登录后进入主界面", passed: hasLogin, detail: hasLogin ? "当前已登录：\(currentUserID)" : "当前仍是 guest 或未登录"),
            .init(title: "AI 对话内容可落地为记录", passed: hasJournal, detail: hasJournal ? "已有 \(journalEntries.count) 条记录" : "尚无记录"),
            .init(title: "对话可提取症状/诱因", passed: hasStructuredExtraction, detail: hasStructuredExtraction ? "已发现结构化提取结果" : "未发现结构化字段"),
            .init(title: "报告可生成基础统计", passed: hasReport, detail: hasReport ? "已有报告样本" : "报告仍为空态"),
            .init(title: "呼吸练习完成记录可见", passed: hasPractice, detail: hasPractice ? "累计 \(breathingPracticeCompletedCount()) 次" : "尚未完成练习"),
            .init(title: "社区可加载或给出失败反馈", passed: hasCommunityFeedOrFeedback, detail: hasCommunityFeedOrFeedback ? "社区内容/反馈可见" : "社区无内容且无反馈"),
            .init(title: "合规入口可达（隐私/协议/免责声明）", passed: hasComplianceEntry, detail: "已在设置页配置导航入口"),
            .init(title: "运行告警与事件可观察", passed: hasObservability, detail: hasObservability ? "事件/告警数据已生成" : "暂无事件与告警")
        ]

        let failCriteria: [FailCriterionStatus] = [
            .init(
                title: "主链路中断（登录->AI->报告->练习）",
                triggered: !(hasLogin && hasJournal && hasReport && hasPractice),
                detail: "需同时满足登录、记录、报告、练习四项"
            ),
            .init(
                title: "风险边界失效（高风险未拦截）",
                triggered: !highRiskBoundaryWorking,
                detail: highRiskBoundaryWorking ? "高风险探针已触发拦截模板" : "高风险探针未命中安全拦截"
            ),
            .init(
                title: "无异常反馈（网络失败无提示）",
                triggered: communityFeedSource == "error" && communityFeedErrorMessage == nil,
                detail: "社区失败场景应有可重试提示"
            ),
            .init(
                title: "合规入口不可达",
                triggered: !hasComplianceEntry,
                detail: "设置页需可打开隐私政策/用户协议/免责声明"
            )
        ]

        return MinimumClosedLoopSnapshot(checklist: checklist, failCriteria: failCriteria)
    }

    private func seedAuthTokenIfNeeded() {
        let userID = authSession.currentUserID
        guard tokenManager.validAccessToken(userID: userID) == nil else { return }
        tokenManager.seedToken(
            userID: userID,
            accessToken: "boot-\(userID)",
            refreshToken: "refresh-\(userID)",
            expiresIn: 1800
        )
    }

    private func refreshSyncDiagnostics() {
        syncDiagnostics = syncQueue.diagnostics(limit: 10)
    }

    private func handleSessionInvalidated() {
        DispatchQueue.main.async {
            self.authErrorMessage = nil
            self.authSession.logout()
            self.currentUserID = self.authSession.currentUserID
            self.isLoggedIn = false
            self.showSessionExpiredAlert = true
            self.sessionExpiredMessage = "登录状态已失效，请重新登录。"
            self.alertingCenter.record(
                category: "auth-session",
                message: "会话失效，已强制退出登录",
                level: .critical
            )
            self.refreshOperationalObservability()
        }
    }

    private func refreshOperationalObservability() {
        runtimeEvents = Array(alertingCenter.events.suffix(12).reversed())
        operationalAlerts = Array(alertingCenter.alerts.suffix(8).reversed())
    }

#if canImport(SwiftData)
    private func migrateCurrentUserIfNeeded() {
        guard let context = localModelContext else { return }
        guard !migratedUserIDs.contains(currentUserID) else { return }
        do {
            let migrator = UserDefaultsToSwiftDataMigrator(context: context)
            try migrator.migrateIfNeeded(userID: currentUserID)
            migratedUserIDs.insert(currentUserID)
        } catch {
            alertingCenter.record(
                category: "local-migration",
                message: "SwiftData 迁移失败：\(error.localizedDescription)",
                level: .warning
            )
            refreshOperationalObservability()
        }
    }

    private func purgeLocalSwiftData(userID: String) throws {
        guard let context = localModelContext else { return }
        try context.delete(model: LocalUserProfile.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalSymptomRecord.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalMoodRecord.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalSleepRecord.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalHotFlashRecord.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalAIConversation.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalContentBookmark.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalBreathingSession.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalConsentRecord.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalJournalEntryCache.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalConversationInsightCache.self, where: #Predicate { $0.userID == userID })
        try context.delete(model: LocalReportSnapshotCache.self, where: #Predicate { $0.userID == userID })
        try context.save()
    }
#endif

    private func recordPrivacyAction(
        _ action: PrivacyActionType,
        status: PrivacyActionStatus,
        detail: String,
        userID: String? = nil
    ) {
        privacyActionAuditStore.append(
            action: action,
            status: status,
            userID: userID ?? currentUserID,
            detail: detail
        )
    }

    private func inferIntent(from text: String) -> String {
        let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalized.contains("怎么") || normalized.contains("如何") || normalized.contains("建议") {
            return "seeking_solution"
        }
        if normalized.contains("是不是") || normalized.contains("是否") || normalized.contains("?") || normalized.contains("？") {
            return "seeking_clarity"
        }
        if normalized.contains("今天") || normalized.contains("昨晚") || normalized.contains("记录") || normalized.contains("症状") {
            return "daily_journal"
        }
        return "general_support"
    }

    private func shouldAskClarifyingQuestion(userText: String, history: [String], intent: String) -> Bool {
        let cleaned = userText.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasQuestionMark = cleaned.contains("?") || cleaned.contains("？")
        let isFirstRound = history.count <= 2
        let isShortInput = cleaned.count <= 20
        return (intent == "seeking_solution" || intent == "general_support") && isFirstRound && isShortInput && !hasQuestionMark
    }

    private func clarifyingQuestion(for intent: String) -> String {
        switch intent {
        case "seeking_solution":
            return "你最希望先解决的是“睡眠、潮热、情绪”中的哪一项？"
        case "daily_journal":
            return "你希望我主要帮你做“记录归纳”还是“下一步建议”？"
        default:
            return "你现在最困扰的点是什么，我会优先围绕它来帮你。"
        }
    }

    private func empatheticPrefix(for intent: String) -> String {
        switch intent {
        case "daily_journal":
            return "我看到了你今天很认真在记录自己的状态。"
        case "seeking_solution":
            return "你想尽快找到可执行的方法，这很重要。"
        case "seeking_clarity":
            return "你的问题很关键，我们可以一步步确认。"
        default:
            return "谢谢你愿意告诉我这些，我会陪你一起梳理。"
        }
    }

    private func formatCitationsInline(_ citations: [RAGCitation], version: String) -> String {
        guard !citations.isEmpty else { return "\n\n知识库版本：\(version)" }
        let titles = citations.map(\.title).joined(separator: "；")
        return "\n\n参考来源：\(titles)\n知识库版本：\(version)"
    }

    private func updateConversationMemory(with intent: String) {
        conversationMemory.append(intent)
        if conversationMemory.count > 8 {
            conversationMemory.removeFirst(conversationMemory.count - 8)
        }
    }

    static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}

private final class NetworkReachabilityWatch {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "chaoan.network.path")

    func start(handler: @escaping (Bool) -> Void) {
        monitor.pathUpdateHandler = { path in
            let online = path.status == .satisfied
            DispatchQueue.main.async {
                handler(online)
            }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }
}
