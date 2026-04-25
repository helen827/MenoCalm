import Foundation
import Combine

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
    case practiceDetail
    case communityPost
    case communityPostDetail
    case userStatus
    case basicInfo
    case selfTest
    case selfTestResult
}

final class AppViewModel: ObservableObject {
    @Published var onboardingStep = 1
    @Published var isLoggedIn = false
    @Published var journalEntries: [JournalEntry] = []
    @Published var conversationInsights: [ConversationInsight] = []
    @Published private(set) var reportSnapshotsByRange: [Int: ReportSnapshot] = [:]
    @Published private(set) var currentUserID = "guest-local"
    @Published private(set) var rolloutSummary = "localReads=0 remoteReads=0 syncSuccess=0 syncFailures=0"
    @Published private(set) var rolloutAlerts: [String] = []
    @Published private(set) var syncDiagnostics = SyncQueueDiagnostics(
        pendingCount: 0,
        runningCount: 0,
        retriedCount: 0,
        failedCount: 0,
        recent: []
    )

    private let journalRepository: JournalRepositoryProtocol
    private let conversationInsightRepository: ConversationInsightRepositoryProtocol
    private let reportSnapshotRepository: ReportSnapshotRepositoryProtocol
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
    private let rolloutThresholds = RolloutThresholds.default

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
        let tokenManager = AuthTokenManager()
        self.tokenManager = tokenManager
        let userIDProvider = { authSession.currentUserID }
        let localRepository = UserDefaultsJournalRepository(userIDProvider: userIDProvider)
        let localInsightRepository = UserDefaultsConversationInsightRepository(userIDProvider: userIDProvider)
        let localReportRepository = UserDefaultsReportSnapshotRepository(userIDProvider: userIDProvider)
        let backendBaseURL = URL(string: UserDefaults.standard.string(forKey: "chaoan_backend_base_url") ?? "")
        let backendEnv = BackendEnvironment(
            baseURL: backendBaseURL,
            timeout: 8,
            tokenProvider: tokenManager
        )
        let remoteRepository = RemoteJournalRepository(
            apiClient: HTTPRemoteJournalAPIClient(env: backendEnv),
            userIDProvider: userIDProvider
        )
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
    }

    func nextOnboarding() {
        onboardingStep = min(4, onboardingStep + 1)
    }

    func login() {
        if authSession.isAnonymous {
            authSession.loginWithPhone("13800000000")
            reloadForCurrentUser()
        }
        isLoggedIn = true
    }

    func loginWithPhone(_ phone: String) {
        authSession.loginWithPhone(phone)
        seedAuthTokenIfNeeded()
        reloadForCurrentUser()
        isLoggedIn = true
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

    private func refreshRolloutSummary() {
        let snapshot = rolloutMonitor.snapshot()
        rolloutSummary = "local=\(snapshot.localReads) remoteOK=\(snapshot.remoteReads) remoteFail=\(snapshot.remoteFailures) syncOK=\(snapshot.syncSuccess) syncFail=\(snapshot.syncFailures)"
        rolloutAlerts = rolloutMonitor.alerts(thresholds: rolloutThresholds)
    }

    func rolloutDashboardSnapshot() -> RolloutSnapshot {
        rolloutMonitor.snapshot()
    }

    func syncDashboardSnapshot() -> SyncQueueDiagnostics {
        syncQueue.diagnostics(limit: 10)
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

    static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
