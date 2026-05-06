//
//  __Tests.swift
//  潮安Tests
//
//  Created by Jiaying He on 2026/4/17.
//

import Testing
import Foundation
@testable import 潮安

struct __Tests {

    @Test
    func extractSignals_detectsSymptomsAndTriggers() {
        let useCase = RuleBasedExtractSignalsUseCase()
        let input = "今天非常忙，开会很多，喝了咖啡，晚上有潮热和失眠，人也很累。"

        let result = useCase.execute(text: input)

        #expect(result.drinks.contains("咖啡"))
        #expect(result.symptoms.contains("潮热"))
        #expect(result.symptoms.contains("失眠"))
        #expect(result.triggers.contains("咖啡因"))
        #expect(result.triggers.contains("压力事件"))
        #expect(result.work.busy == true)
        #expect(result.work.tired == true)
        #expect(result.work.level == 3)
    }

    @Test
    func communityFeed_decodesJSONPayloadShape() throws {
        let json = """
        {"official":[{"id":"1","title":"示例","tag":"官方科普","cardHeight":130,"isOfficial":true}],"user":[{"id":"2","title":"分享","tag":"用户分享","cardHeight":150,"isOfficial":false}]}
        """
        let file = try JSONDecoder().decode(CommunityFeedFile.self, from: Data(json.utf8))
        #expect(file.official.count == 1)
        #expect(file.user.count == 1)
        #expect(file.official[0].heightPoints == 130)
    }

    @Test
    func extractSignals_handlesEmptyText() {
        let useCase = RuleBasedExtractSignalsUseCase()

        let result = useCase.execute(text: "")

        #expect(result.drinks.isEmpty)
        #expect(result.symptoms.isEmpty)
        #expect(result.triggers.isEmpty)
        #expect(result.work.busy == false)
        #expect(result.work.tired == false)
        #expect(result.work.level == 1)
        #expect(result.events.isEmpty)
    }

    @Test
    func extractSignals_handlesPunctuationOnlyText() {
        let useCase = RuleBasedExtractSignalsUseCase()

        let result = useCase.execute(text: "！！！，，，。。。")

        #expect(result.drinks.isEmpty)
        #expect(result.symptoms.isEmpty)
        #expect(result.triggers.isEmpty)
        #expect(result.work.busy == false)
        #expect(result.work.tired == false)
        #expect(result.work.level == 1)
    }

    @Test
    func extractSignals_handlesLongTextWithRepeatedKeywords() {
        let useCase = RuleBasedExtractSignalsUseCase()
        let repeatedBusy = Array(repeating: "非常忙，", count: 40).joined()
        let repeatedCoffee = Array(repeating: "咖啡，", count: 40).joined()
        let longInput = "\(repeatedBusy)\(repeatedCoffee)晚上潮热、失眠、焦虑，还觉得累。"

        let result = useCase.execute(text: longInput)

        #expect(result.drinks == ["咖啡"])
        #expect(result.symptoms == ["潮热", "失眠", "焦虑"])
        #expect(result.triggers.contains("咖啡因"))
        #expect(result.triggers.contains("压力事件"))
        #expect(result.work.busy == true)
        #expect(result.work.tired == true)
        #expect(result.work.level == 3)
        #expect(result.events.count == 3)
    }

    @Test
    func extractSignals_deduplicatesTriggersWhenKeywordsRepeat() {
        let useCase = RuleBasedExtractSignalsUseCase()
        let input = "咖啡 咖啡 咖啡，今天开会很忙很赶，晚饭很辣。"

        let result = useCase.execute(text: input)

        #expect(result.triggers == ["咖啡因", "压力事件", "辛辣饮食"])
    }

    @Test
    func appRouter_managesTabAndPath() {
        let router = AppRouter()

        router.push(.trendReport)
        #expect(router.path == [.trendReport])

        router.push(.medicalList)
        #expect(router.path == [.trendReport, .medicalList])

        router.pop()
        #expect(router.path == [.trendReport])

        router.resetToTab(.profile)
        #expect(router.selectedTab == .profile)
        #expect(router.path.isEmpty)
    }

    @Test
    func appViewModel_savesEntryViaUseCaseAndRepository() {
        let repository = MockJournalRepository()
        let useCase = MockExtractSignalsUseCase()
        let insightRepository = MockConversationInsightRepository()
        let extractor = MockConversationExtractor()
        let vm = AppViewModel(
            journalRepository: repository,
            conversationInsightRepository: insightRepository,
            extractSignalsUseCase: useCase,
            conversationExtractor: extractor
        )

        vm.saveJournal(text: "记录：今天有点失眠")

        #expect(vm.journalEntries.count == 1)
        #expect(vm.conversationInsights.count == 1)
        #expect(repository.savedEntries.count == 1)
        #expect(insightRepository.savedInsights.count == 1)
        #expect(repository.savedEntries.first?.rawText == "记录：今天有点失眠")
        #expect(insightRepository.savedInsights.first?.sourceText == "记录：今天有点失眠")
        #expect(useCase.receivedTexts == ["记录：今天有点失眠"])
    }

    @Test
    func appViewModel_ignoresBlankJournal() {
        let repository = MockJournalRepository()
        let useCase = MockExtractSignalsUseCase()
        let insightRepository = MockConversationInsightRepository()
        let extractor = MockConversationExtractor()
        let vm = AppViewModel(
            journalRepository: repository,
            conversationInsightRepository: insightRepository,
            extractSignalsUseCase: useCase,
            conversationExtractor: extractor
        )

        vm.saveJournal(text: "   \n")

        #expect(vm.journalEntries.isEmpty)
        #expect(vm.conversationInsights.isEmpty)
        #expect(repository.savedEntries.isEmpty)
        #expect(insightRepository.savedInsights.isEmpty)
        #expect(useCase.receivedTexts.isEmpty)
    }

    @Test
    func appViewModel_topSymptomsAndTopTriggers_returnsRankedResults() {
        let repository = MockJournalRepository()
        let insightRepository = MockConversationInsightRepository()
        repository.storedEntries = [
            makeJournalEntry(
                id: "1",
                symptoms: ["失眠", "潮热"],
                triggers: ["压力事件", "咖啡因"]
            ),
            makeJournalEntry(
                id: "2",
                symptoms: ["失眠", "焦虑"],
                triggers: ["压力事件", "辛辣饮食"]
            ),
            makeJournalEntry(
                id: "3",
                symptoms: ["失眠", "潮热"],
                triggers: ["压力事件"]
            )
        ]

        let vm = AppViewModel(
            journalRepository: repository,
            conversationInsightRepository: insightRepository,
            extractSignalsUseCase: MockExtractSignalsUseCase()
        )

        #expect(vm.topSymptoms(limit: 2) == ["失眠", "潮热"])
        let topTriggers = vm.topTriggers(limit: 2)
        #expect(topTriggers.first == "压力事件")
        #expect(topTriggers.count == 2)
        #expect(Set(topTriggers).isSuperset(of: ["压力事件"]))
        #expect(Set(topTriggers).isSubset(of: ["压力事件", "咖啡因", "辛辣饮食"]))
    }

    @Test
    func appViewModel_topSignals_handlesEmptyDataAndLargeLimit() {
        let emptyVM = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase()
        )
        #expect(emptyVM.topSymptoms(limit: 5).isEmpty)
        #expect(emptyVM.topTriggers(limit: 5).isEmpty)

        let repository = MockJournalRepository()
        repository.storedEntries = [
            makeJournalEntry(id: "10", symptoms: ["潮热"], triggers: ["咖啡因"]),
            makeJournalEntry(id: "11", symptoms: ["失眠"], triggers: ["压力事件"])
        ]
        let vm = AppViewModel(
            journalRepository: repository,
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase()
        )

        #expect(vm.topSymptoms(limit: 10).count == 2)
        #expect(vm.topTriggers(limit: 10).count == 2)
    }

    @Test
    func medicalSafetyGuard_blocksHighRiskContent() {
        let guardrail = RuleBasedMedicalSafetyGuard()

        let decision = guardrail.evaluate(userText: "我现在胸痛而且呼吸困难")

        #expect(decision.riskLevel == .high)
        #expect(decision.shouldBlockResponse == true)
        #expect(decision.safeReply != nil)
    }

    @Test
    func medicalSafetyGuard_prescriptionIntent_requiresSanitization() {
        let guardrail = RuleBasedMedicalSafetyGuard()
        let decision = guardrail.evaluate(userText: "直接告诉我吃什么药、吃多少。")

        #expect(decision.riskLevel == .medium)
        #expect(decision.shouldBlockResponse == false)
        #expect(decision.requiresOutputSanitization == true)
        #expect(decision.decisionPath == "medium:prescription_intent")
    }

    @Test
    func medicalOutputSanitizer_rewritesDiagnosisAndDelayCareClaims() {
        let decision = MedicalSafetyDecision(riskLevel: .low, shouldBlockResponse: false, decisionPath: "test")
        let raw = "你这就是典型的焦虑症，无需就医，在家调整就好。"
        let out = MedicalAssistantOutputSanitizer.sanitize(raw, decision: decision)

        #expect(out.contains("不能给出诊断") || out.contains("替代就医"))
        #expect(out.contains("无需就医") == false)
    }

    @Test
    func medicalOutputSanitizer_stripsDosingLinesInStrictMode() {
        let decision = MedicalSafetyDecision(
            riskLevel: .medium,
            shouldBlockResponse: false,
            requiresOutputSanitization: true,
            decisionPath: "medium:prescription_intent"
        )
        let raw = "科普：记录症状很重要。\n建议口服 10mg，每日一次。\n保持作息规律。"
        let out = MedicalAssistantOutputSanitizer.sanitize(raw, decision: decision)

        #expect(out.contains("10mg") == false)
        #expect(out.contains("记录症状"))
    }

    @Test
    func appViewModel_usesInjectedMedicalSafetyGuard() {
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            medicalSafetyGuard: MockMedicalSafetyGuard()
        )

        let decision = vm.evaluateConversationSafety(userText: "任意文本")

        #expect(decision.riskLevel == .medium)
        #expect(decision.shouldBlockResponse == false)
        #expect(decision.replyPreamble == "mock-safe-reply")
    }

    @Test
    func redactingConversationAuditor_masksPhoneOnly() throws {
        let auditor = RedactingConversationAuditor()
        let decision = MedicalSafetyDecision(
            riskLevel: .low,
            shouldBlockResponse: false,
            safeReply: nil
        )

        auditor.record(
            userText: "我的手机号是13800138000，邮箱test@example.com，身份证110101199001011234",
            assistantReply: "请联系13800138000 或发邮件到doctor@example.com",
            decision: decision
        )

        #expect(auditor.events.count == 1)
        let event = try #require(auditor.events.first)
        #expect(event.userText.contains("[已脱敏手机号]"))
        #expect(event.assistantReply.contains("[已脱敏手机号]"))
        #expect(event.userText.contains("test@example.com"))
        #expect(event.userText.contains("110101199001011234"))
        #expect(event.assistantReply.contains("doctor@example.com"))
        #expect(event.decisionPath.isEmpty)
    }

    @Test
    func appViewModel_auditsConversationWithDecision() throws {
        let mockAuditor = MockConversationAuditor()
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            medicalSafetyGuard: MockMedicalSafetyGuard(),
            conversationAuditor: mockAuditor
        )
        let decision = MedicalSafetyDecision(
            riskLevel: .high,
            shouldBlockResponse: true,
            safeReply: "请就医"
        )

        vm.auditConversation(userText: "胸痛", assistantReply: "请就医", decision: decision)

        #expect(mockAuditor.records.count == 1)
        let record = try #require(mockAuditor.records.first)
        #expect(record.userText == "胸痛")
        #expect(record.assistantReply == "请就医")
        #expect(record.decision.shouldBlockResponse == true)
    }

    @Test
    func ragService_returnsAnswerWithCitations() {
        let service = RAGService()

        let answer = service.generateReply(userText: "最近潮热比较频繁")

        #expect(!answer.text.isEmpty)
        #expect(answer.citations.count == 1)
        #expect(answer.citations.first?.sourceId == "kb-guideline-menopause-basic")
        #expect(answer.knowledgeBaseVersion == "kb-v1-local")
    }

    @Test
    func appViewModel_usesInjectedRAGService() {
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            medicalSafetyGuard: MockMedicalSafetyGuard(),
            conversationAuditor: MockConversationAuditor(),
            ragService: MockRAGService()
        )

        let answer = vm.generateRAGAssistantReply(userText: "测试输入")

        #expect(answer.text == "mock-rag-answer")
        #expect(answer.citations == [RAGCitation(sourceId: "mock-source", title: "mock-title", knowledgeBaseVersion: "mock-kb-v1")])
        #expect(answer.knowledgeBaseVersion == "mock-kb-v1")
    }

    @Test
    func a1_guidedReply_asksClarifyingQuestionForShortSolutionIntent() {
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            ragService: MockRAGService()
        )

        let reply = vm.generateGuidedAssistantReply(userText: "怎么缓解", history: ["AI开场"])

        #expect(reply.contains("先确认一下"))
        #expect(reply.contains("你最希望先解决的是"))
    }

    @Test
    func a4_strategyToggle_offBypassesClarifyingQuestion() {
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            ragService: MockRAGService()
        )
        vm.setGuidedConversationEnabled(false)

        let reply = vm.generateGuidedAssistantReply(userText: "怎么缓解", history: ["AI开场"])

        #expect(reply.contains("mock-rag-answer"))
        #expect(reply.contains("先确认一下") == false)
    }

    @Test
    func a3_conversationMemory_keepsRecentIntentWindow() {
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            ragService: MockRAGService()
        )

        for _ in 0..<10 {
            _ = vm.generateGuidedAssistantReply(userText: "今天记录一下", history: [])
        }

        #expect(vm.conversationMemory.count == 8)
    }

    @Test
    func ruleBasedConversationExtractor_buildsLifestyleEvents() {
        let extractor = RuleBasedConversationExtractor()
        let extracted = ExtractedData(
            text: "今天很忙，喝了咖啡，而且很累",
            foods: [],
            drinks: ["咖啡"],
            work: WorkData(busy: true, tired: true, level: 2),
            symptoms: ["失眠"],
            events: [SymptomEvent(time: "今日", symptom: "失眠")],
            triggers: ["压力事件", "咖啡因"]
        )

        let insight = extractor.extractInsight(from: extracted.text, createdAt: 1000, extracted: extracted)

        #expect(insight.symptoms == ["失眠"])
        #expect(insight.triggers == ["压力事件", "咖啡因"])
        #expect(insight.lifestyleEvents.contains(LifestyleEvent(type: "饮品", value: "咖啡")))
        #expect(insight.lifestyleEvents.contains(LifestyleEvent(type: "工作压力", value: "忙碌等级2")))
        #expect(insight.lifestyleEvents.contains(LifestyleEvent(type: "体感", value: "疲惫")))
    }

    @Test
    func ruleBasedCorrelationAnalyzer_buildsCorrelationsAndReminder() {
        let analyzer = RuleBasedCorrelationAnalyzer()
        let now: TimeInterval = 1_000_000
        let dayMillis: TimeInterval = 24 * 60 * 60 * 1000
        let insights = [
            ConversationInsight(
                id: "1",
                sourceText: "记录1",
                createdAt: now - dayMillis,
                symptoms: ["失眠", "心悸"],
                triggers: ["压力事件"],
                lifestyleEvents: [LifestyleEvent(type: "饮品", value: "咖啡")]
            ),
            ConversationInsight(
                id: "2",
                sourceText: "记录2",
                createdAt: now - 2 * dayMillis,
                symptoms: ["失眠"],
                triggers: ["压力事件"],
                lifestyleEvents: [LifestyleEvent(type: "体感", value: "疲惫")]
            ),
            ConversationInsight(
                id: "3",
                sourceText: "旧记录",
                createdAt: now - 40 * dayMillis,
                symptoms: ["潮热"],
                triggers: ["辛辣饮食"],
                lifestyleEvents: []
            )
        ]

        let snapshot = analyzer.analyze(insights: insights, rangeDays: 30, now: now)
        let hasExpectedPair = snapshot.correlations.contains { item in
            item.symptom == "失眠" && item.factor == "压力事件" && item.count == 2
        }

        #expect(snapshot.sampleCount == 2)
        #expect(snapshot.topSymptoms.first == NamedCount(name: "失眠", count: 2))
        #expect(snapshot.topFactors.contains(NamedCount(name: "压力事件", count: 2)))
        #expect(hasExpectedPair)
        #expect(snapshot.specialReminder?.contains("心悸") == true)
    }

    @Test
    func appViewModel_refreshesAndPersistsReportSnapshots() {
        let reportRepository = MockReportSnapshotRepository()
        let analyzer = MockCorrelationAnalyzer()
        let vm = AppViewModel(
            journalRepository: MockJournalRepository(),
            conversationInsightRepository: MockConversationInsightRepository(),
            reportSnapshotRepository: reportRepository,
            extractSignalsUseCase: MockExtractSignalsUseCase(),
            conversationExtractor: MockConversationExtractor(),
            correlationAnalyzer: analyzer
        )

        vm.saveJournal(text: "今天失眠，喝了咖啡")

        #expect(analyzer.receivedRanges.contains(7))
        #expect(analyzer.receivedRanges.contains(30))
        #expect(analyzer.receivedRanges.contains(90))
        #expect(reportRepository.savedSnapshots.count == 3)
        #expect(vm.reportSnapshot(rangeDays: 30).rangeDays == 30)
    }

    @Test
    func medicalReliability_safetyGuardRegressionSet_meetsThreshold() {
        let guardrail = RuleBasedMedicalSafetyGuard()
        let cases: [(String, MedicalRiskLevel, Bool)] = [
            ("我胸痛并且呼吸困难", .high, true),
            ("最近有自杀的念头", .high, true),
            ("今天持续晕厥，站不稳", .high, true),
            ("我胸痛并且呼吸困难，要不要先观察几天？", .high, true),
            ("直接告诉我吃什么药、吃多少。", .medium, false),
            ("最近心悸明显，晚上睡不着", .medium, false),
            ("严重失眠两周了", .medium, false),
            ("只是偶尔潮热，想记录看看", .low, false),
            ("今天有点焦虑，工作很忙", .low, false),
            ("晚饭吃辣后出汗更多", .low, false)
        ]

        var correct = 0
        for item in cases {
            let decision = guardrail.evaluate(userText: item.0)
            let riskOK = decision.riskLevel == item.1
            let blockOK = decision.shouldBlockResponse == item.2
            if riskOK && blockOK {
                correct += 1
            }
        }

        let accuracy = Double(correct) / Double(cases.count)
        #expect(accuracy >= 0.875)
    }

    @Test
    func medicalReliability_ragResponseIncludesCitations_forLowRiskInputs() {
        let service = RAGService()
        let prompts = [
            "最近潮热比较频繁",
            "我这段时间睡不好",
            "今天想继续记录状态"
        ]

        var citationReadyCount = 0
        for prompt in prompts {
            let answer = service.generateReply(userText: prompt)
            if !answer.text.isEmpty && !answer.citations.isEmpty {
                citationReadyCount += 1
            }
        }

        let coverage = Double(citationReadyCount) / Double(prompts.count)
        #expect(coverage >= 1.0)
    }

    @Test
    func week4_medicalReliability_datasetGate_meetsThresholds() {
        struct DatasetCase {
            let text: String
            let expectBlocked: Bool
            let expectCitation: Bool
        }

        let guardrail = RuleBasedMedicalSafetyGuard()
        let rag = RAGService()
        let dataset: [DatasetCase] = [
            .init(text: "最近潮热和失眠反复，想知道怎么记录", expectBlocked: false, expectCitation: true),
            .init(text: "心情焦虑，晚上出汗多", expectBlocked: false, expectCitation: false),
            .init(text: "胸口剧痛并且呼吸困难，应该怎么办", expectBlocked: true, expectCitation: false),
            .init(text: "我有强烈伤害自己冲动", expectBlocked: true, expectCitation: false),
            .init(text: "想了解咖啡因是否会加重潮热", expectBlocked: false, expectCitation: false),
            .init(text: "突然大出血，止不住，很害怕", expectBlocked: true, expectCitation: false),
            .init(text: "今天天气不错，随便记一下心情还可以", expectBlocked: false, expectCitation: false)
        ]

        var positives = 0
        var truePositives = 0
        var negatives = 0
        var falsePositives = 0
        var citationExpectedCount = 0
        var citationCoveredCount = 0

        for sample in dataset {
            let decision = guardrail.evaluate(userText: sample.text)
            let answer = rag.generateReply(userText: sample.text)

            if sample.expectBlocked {
                positives += 1
                if decision.shouldBlockResponse {
                    truePositives += 1
                }
            } else {
                negatives += 1
                if decision.shouldBlockResponse {
                    falsePositives += 1
                }
            }

            if sample.expectCitation {
                citationExpectedCount += 1
                if !answer.citations.isEmpty {
                    citationCoveredCount += 1
                }
            }
        }

        let recall = positives == 0 ? 1.0 : Double(truePositives) / Double(positives)
        let falsePositiveRate = negatives == 0 ? 0.0 : Double(falsePositives) / Double(negatives)
        let citationCoverage = citationExpectedCount == 0 ? 1.0 : Double(citationCoveredCount) / Double(citationExpectedCount)

        #expect(recall >= 1.0)
        #expect(falsePositiveRate <= 0.35)
        #expect(citationCoverage >= 1.0)
    }

    @Test
    func week2_userScopedRepository_isolatesEntriesByUser() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)

        let userARepo = UserDefaultsJournalRepository(defaults: defaults, userIDProvider: { "userA" })
        let userBRepo = UserDefaultsJournalRepository(defaults: defaults, userIDProvider: { "userB" })

        userARepo.saveEntries([makeJournalEntry(id: "a1", symptoms: ["失眠"], triggers: ["压力事件"])])
        userBRepo.saveEntries([makeJournalEntry(id: "b1", symptoms: ["潮热"], triggers: ["咖啡因"])])

        #expect(userARepo.loadEntries().map(\.id) == ["a1"])
        #expect(userBRepo.loadEntries().map(\.id) == ["b1"])
    }

    @Test
    func week2_userScopedRepository_migratesLegacyEntriesOnFirstLoad() throws {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)

        let legacyEntries = [makeJournalEntry(id: "legacy-1", symptoms: ["失眠"], triggers: ["压力事件"])]
        let legacyData = try JSONEncoder().encode(legacyEntries)
        defaults.set(legacyData, forKey: StorageKeys.entries)

        let scopedRepo = UserDefaultsJournalRepository(defaults: defaults, userIDProvider: { "migratedUser" })
        let loaded = scopedRepo.loadEntries()

        #expect(loaded.map(\.id) == ["legacy-1"])
        let scopedKey = StorageKeys.scoped(StorageKeys.entries, userID: "migratedUser")
        #expect(defaults.data(forKey: scopedKey) != nil)
    }

    @Test
    func week2_syncQueue_retriesAndEventuallySucceeds() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let queue = SyncQueue(maxRetries: 3, defaults: defaults, userIDProvider: { "sync-user" })
        var attempts = 0
        let entries = [makeJournalEntry(id: "sync-1", symptoms: ["失眠"], triggers: ["压力事件"])]
        queue.enqueueUpload(entries: entries)

        let uploader: ([JournalEntry]) throws -> Void = { _ in
            attempts += 1
            if attempts < 3 {
                throw RemoteAPIError.injectedFailure
            }
        }
        queue.flush(uploader: uploader)
        queue.flush(uploader: uploader)
        queue.flush(uploader: uploader)

        #expect(attempts == 3)
        #expect(queue.pendingTasks.isEmpty)
        #expect(queue.failedJobIDs.isEmpty)
    }

    @Test
    func week3_syncQueue_persistsPendingTasksAcrossReinit() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let entries = [makeJournalEntry(id: "persist-1", symptoms: ["潮热"], triggers: ["咖啡因"])]
        var queue = SyncQueue(maxRetries: 2, defaults: defaults, userIDProvider: { "persist-user" })
        queue.enqueueUpload(entries: entries)

        queue = SyncQueue(maxRetries: 2, defaults: defaults, userIDProvider: { "persist-user" })

        #expect(queue.pendingTasks.count == 1)
        #expect(queue.pendingTasks.first?.entries.first?.id == "persist-1")
    }

    @Test
    func week4_syncQueue_tracksLifecycleTransitionsAndDiagnostics() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let queue = SyncQueue(maxRetries: 2, defaults: defaults, userIDProvider: { "diag-user" })
        queue.enqueueUpload(entries: [makeJournalEntry(id: "diag-1", symptoms: ["失眠"], triggers: ["压力"])], id: "diag-task")

        var attempts = 0
        let failingUploader: ([JournalEntry]) throws -> Void = { _ in
            attempts += 1
            throw RemoteAPIError.timeout
        }
        queue.flush(uploader: failingUploader)
        queue.flush(uploader: failingUploader)

        let diagnostics = queue.diagnostics(limit: 10)
        let statuses = diagnostics.recent.map(\.status)

        #expect(attempts == 2)
        #expect(diagnostics.failedCount == 1)
        #expect(statuses.contains(.pending))
        #expect(statuses.contains(.running))
        #expect(statuses.contains(.retried))
        #expect(statuses.contains(.failed))
    }

    @Test
    func week2_repositoryFacade_failOpen_returnsLocalWhenRemoteFails() {
        let local = MockJournalRepository()
        local.storedEntries = [makeJournalEntry(id: "local-1", symptoms: ["焦虑"], triggers: ["压力事件"])]
        let api = FailingRemoteJournalAPIClient()
        let remote = RemoteJournalRepository(apiClient: api, userIDProvider: { "userA" })
        let flags = FeatureFlags()
        flags.cloudReadEnabled = true
        flags.failOpenToLocalData = true
        let facade = RepositoryFacade(
            localRepository: local,
            remoteRepository: remote,
            syncQueue: SyncQueue(),
            featureFlags: flags
        )

        let loaded = facade.loadEntries()

        #expect(loaded.map(\.id) == ["local-1"])
    }

    @Test
    func week3_httpRemoteClient_mapsHttpStatusCodes() {
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(200) == nil)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(400) == .badRequest)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(401) == .unauthorized)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(403) == .forbidden)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(404) == .notFound)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(409) == .conflict)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(429) == .rateLimited)
        #expect(HTTPRemoteJournalAPIClient.mapStatusCode(500) == .server(statusCode: 500))
    }

    @Test
    func week3_httpRemoteClient_throwsWhenEndpointMissing() {
        let client = HTTPRemoteJournalAPIClient(env: .unconfigured)

        #expect(throws: RemoteAPIError.endpointNotConfigured) {
            _ = try client.fetchEntries(userID: "u1")
        }
    }

    @Test
    func week4_authTokenManager_refreshesExpiredToken() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let manager = AuthTokenManager(defaults: defaults, nowProvider: { 1000 })
        manager.seedToken(userID: "u1", accessToken: "expired", refreshToken: "r1", expiresIn: -10)

        let refreshed = manager.validAccessToken(userID: "u1")

        #expect(refreshed?.contains("acc-u1-") == true)
    }

    @Test
    func day1_authTokenManager_loginWithPhone_storesUsableToken() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let manager = AuthTokenManager(defaults: defaults, authAPIClient: InMemoryAuthAPIClient())

        let success = manager.loginWithPhone("13800138000")
        let accessToken = manager.validAccessToken(userID: "phone_13800138000")

        #expect(success == true)
        #expect(accessToken?.contains("acc-13800138000") == true)
    }

    @Test
    func day3_runtimeConfigResolver_usesEnvironmentScopedEndpoint() throws {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        defaults.set("staging", forKey: "chaoan_runtime_env")
        defaults.set("https://staging.example.com", forKey: "chaoan_backend_base_url_staging")
        defaults.set(12.0, forKey: "chaoan_backend_timeout_seconds")

        let config = try RuntimeConfigResolver(defaults: defaults).resolve()

        #expect(config.environment == .staging)
        #expect(config.backend.baseURL?.absoluteString == "https://staging.example.com")
        #expect(config.backend.timeout == 12.0)
    }

    @Test
    func day3_runtimeConfigResolver_rejectsInsecureProductionEndpoint() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        defaults.set("prod", forKey: "chaoan_runtime_env")
        defaults.set("http://localhost:8080", forKey: "chaoan_backend_base_url_prod")

        #expect(throws: RuntimeConfigError.insecureProductionEndpoint) {
            _ = try RuntimeConfigResolver(defaults: defaults).resolve()
        }
    }

    @Test
    func day2_authTokenManager_refreshFailure_invalidatesSessionAndEmitsCallback() {
        let suite = "test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let authClient = StubAuthAPIClient(
            loginResult: .success(
                AuthTokenPayload(
                    accessToken: "acc-u1",
                    refreshToken: "ref-u1",
                    expiresIn: -5,
                    userID: "u1"
                )
            ),
            refreshResult: .failure(.unauthorized)
        )
        let manager = AuthTokenManager(defaults: defaults, nowProvider: { 1000 }, authAPIClient: authClient)
        var invalidatedUser: String?
        manager.onSessionInvalidated = { userID in
            invalidatedUser = userID
        }

        let success = manager.loginWithPhone("13800138000")
        let tokenAfterRefresh = manager.validAccessToken(userID: "u1")

        #expect(success == true)
        #expect(tokenAfterRefresh == nil)
        #expect(invalidatedUser == "u1")
    }

    @Test
    func week3_ragService_supportsKnowledgeBaseHotUpdateAndVersionTag() {
        let service = RAGService()
        service.updateKnowledgeBase(
            documents: [
                KnowledgeDocument(
                    sourceId: "kb-v2-hotflash",
                    title: "潮热行为干预 V2",
                    keywords: ["潮热"],
                    response: "V2 建议：先补水、再降温，并在 2 周内记录触发场景。"
                )
            ],
            version: "kb-v2-remote"
        )

        let answer = service.generateReply(userText: "今天潮热很明显")

        #expect(answer.text.contains("V2 建议"))
        #expect(answer.knowledgeBaseVersion == "kb-v2-remote")
        #expect(answer.citations.first?.knowledgeBaseVersion == "kb-v2-remote")
        #expect(answer.citations.first?.sourceId == "kb-v2-hotflash")
    }

    @Test
    func week3_conflictResolver_prefersNewerAndMergesFields() throws {
        let resolver = JournalConflictResolver()
        let local = JournalEntry(
            id: "same-id",
            date: "2026-04-20",
            rawText: "本地文本",
            createdAt: 1000,
            extracted: ExtractedData(
                text: "本地提取",
                foods: ["辛辣"],
                drinks: ["咖啡"],
                work: WorkData(busy: false, tired: true, level: 1),
                symptoms: ["失眠"],
                events: [SymptomEvent(time: "晚间", symptom: "失眠")],
                triggers: ["咖啡因"]
            )
        )
        let remote = JournalEntry(
            id: "same-id",
            date: "2026-04-21",
            rawText: "远端文本",
            createdAt: 2000,
            extracted: ExtractedData(
                text: "远端提取",
                foods: ["甜食"],
                drinks: ["浓茶"],
                work: WorkData(busy: true, tired: false, level: 3),
                symptoms: ["潮热"],
                events: [SymptomEvent(time: "白天", symptom: "潮热")],
                triggers: ["压力事件"]
            )
        )

        let merged = resolver.resolve(local: [local], remote: [remote])
        let item = try #require(merged.first)

        #expect(merged.count == 1)
        #expect(item.createdAt == 2000)
        #expect(item.rawText == "远端文本")
        #expect(Set(item.extracted.symptoms).isSuperset(of: ["失眠", "潮热"]))
        #expect(Set(item.extracted.triggers).isSuperset(of: ["咖啡因", "压力事件"]))
        #expect(item.extracted.work.level == 3)
        #expect(item.extracted.work.busy == true)
        #expect(item.extracted.work.tired == true)
    }

    @Test
    func week4_conflictResolver_prefersHigherRevisionOverTimestamp() throws {
        let resolver = JournalConflictResolver()
        let local = JournalEntry(
            id: "same-id",
            date: "2026-04-21",
            rawText: "local-v2",
            createdAt: 2000,
            revision: 2,
            extracted: ExtractedData(
                text: "local-v2",
                foods: [],
                drinks: [],
                work: WorkData(busy: false, tired: false, level: 1),
                symptoms: ["失眠"],
                events: [],
                triggers: ["压力事件"]
            )
        )
        let remote = JournalEntry(
            id: "same-id",
            date: "2026-04-22",
            rawText: "remote-v3",
            createdAt: 1000,
            revision: 3,
            extracted: ExtractedData(
                text: "remote-v3",
                foods: [],
                drinks: [],
                work: WorkData(busy: true, tired: true, level: 3),
                symptoms: ["潮热"],
                events: [],
                triggers: ["咖啡因"]
            )
        )

        let merged = resolver.resolve(local: [local], remote: [remote])
        let item = try #require(merged.first)

        #expect(item.rawText == "remote-v3")
        #expect(item.revision == 3)
    }

    @Test
    func week3_repositoryFacade_usesConflictResolverForSameEntry() {
        let local = MockJournalRepository()
        local.storedEntries = [
            JournalEntry(
                id: "same-id",
                date: "2026-04-20",
                rawText: "local",
                createdAt: 1000,
                extracted: ExtractedData(
                    text: "local",
                    foods: [],
                    drinks: ["咖啡"],
                    work: WorkData(busy: false, tired: false, level: 1),
                    symptoms: ["失眠"],
                    events: [SymptomEvent(time: "夜", symptom: "失眠")],
                    triggers: ["咖啡因"]
                )
            )
        ]
        let remoteAPI = StubRemoteJournalAPIClient(
            fetchResult: [
                JournalEntryDTO(
                    id: "same-id",
                    date: "2026-04-21",
                    rawText: "remote",
                    createdAt: 2000,
                    extracted: ExtractedData(
                        text: "remote",
                        foods: [],
                        drinks: ["浓茶"],
                        work: WorkData(busy: true, tired: false, level: 2),
                        symptoms: ["潮热"],
                        events: [SymptomEvent(time: "日", symptom: "潮热")],
                        triggers: ["压力事件"]
                    )
                )
            ]
        )
        let remote = RemoteJournalRepository(apiClient: remoteAPI, userIDProvider: { "u1" })
        let flags = FeatureFlags()
        flags.cloudReadEnabled = true
        let facade = RepositoryFacade(
            localRepository: local,
            remoteRepository: remote,
            syncQueue: SyncQueue(),
            featureFlags: flags
        )

        let merged = facade.loadEntries()

        #expect(merged.count == 1)
        #expect(Set(merged[0].extracted.symptoms).isSuperset(of: ["失眠", "潮热"]))
        #expect(Set(merged[0].extracted.triggers).isSuperset(of: ["咖啡因", "压力事件"]))
    }

    @Test
    func week3_rolloutMonitor_triggersAlertsWhenThresholdExceeded() {
        let monitor = RolloutMonitor()
        monitor.markSyncFailure()
        monitor.markSyncFailure()
        monitor.markSyncSuccess()
        monitor.markRemoteFailure()
        monitor.markRemoteFailure()
        monitor.markRemoteRead()

        let alerts = monitor.alerts(
            thresholds: RolloutThresholds(
                maxSyncFailureRate: 0.30,
                maxRemoteFailureRate: 0.50
            )
        )

        #expect(alerts.count == 2)
        #expect(alerts.joined(separator: " ").contains("同步失败率过高"))
        #expect(alerts.joined(separator: " ").contains("远端读取失败率过高"))
    }

}

private final class MockJournalRepository: JournalRepositoryProtocol {
    var storedEntries: [JournalEntry] = []
    var savedEntries: [JournalEntry] = []

    func loadEntries() -> [JournalEntry] {
        storedEntries
    }

    func saveEntries(_ entries: [JournalEntry]) {
        savedEntries = entries
        storedEntries = entries
    }
}

private final class MockExtractSignalsUseCase: ExtractSignalsUseCaseProtocol {
    var receivedTexts: [String] = []

    func execute(text: String) -> ExtractedData {
        receivedTexts.append(text)
        return ExtractedData(
            text: text,
            foods: [],
            drinks: [],
            work: WorkData(busy: false, tired: false, level: 1),
            symptoms: ["失眠"],
            events: [SymptomEvent(time: "今日", symptom: "失眠")],
            triggers: []
        )
    }
}

private struct MockMedicalSafetyGuard: MedicalSafetyGuardProtocol {
    func evaluate(userText: String) -> MedicalSafetyDecision {
        MedicalSafetyDecision(
            riskLevel: .medium,
            shouldBlockResponse: false,
            replyPreamble: "mock-safe-reply"
        )
    }
}

private final class MockConversationAuditor: ConversationAuditorProtocol {
    struct Record {
        let userText: String
        let assistantReply: String
        let decision: MedicalSafetyDecision
    }

    var records: [Record] = []

    func record(
        userText: String,
        assistantReply: String,
        decision: MedicalSafetyDecision
    ) {
        records.append(Record(userText: userText, assistantReply: assistantReply, decision: decision))
    }
}

private struct MockRAGService: RAGServiceProtocol {
    func generateReply(userText: String) -> RAGAnswer {
        RAGAnswer(
            text: "mock-rag-answer",
            citations: [RAGCitation(sourceId: "mock-source", title: "mock-title", knowledgeBaseVersion: "mock-kb-v1")],
            knowledgeBaseVersion: "mock-kb-v1"
        )
    }

    func updateKnowledgeBase(documents: [KnowledgeDocument], version: String) {}

    func currentKnowledgeBaseVersion() -> String {
        "mock-kb-v1"
    }
}

private final class MockConversationInsightRepository: ConversationInsightRepositoryProtocol {
    var storedInsights: [ConversationInsight] = []
    var savedInsights: [ConversationInsight] = []

    func loadInsights() -> [ConversationInsight] {
        storedInsights
    }

    func saveInsights(_ insights: [ConversationInsight]) {
        savedInsights = insights
        storedInsights = insights
    }
}

private struct MockConversationExtractor: ConversationExtractorProtocol {
    func extractInsight(
        from text: String,
        createdAt: TimeInterval,
        extracted: ExtractedData
    ) -> ConversationInsight {
        ConversationInsight(
            id: String(Int(createdAt)),
            sourceText: text,
            createdAt: createdAt,
            symptoms: extracted.symptoms,
            triggers: extracted.triggers,
            lifestyleEvents: []
        )
    }
}

private final class MockReportSnapshotRepository: ReportSnapshotRepositoryProtocol {
    var storedSnapshots: [ReportSnapshot] = []
    var savedSnapshots: [ReportSnapshot] = []

    func loadSnapshots() -> [ReportSnapshot] {
        storedSnapshots
    }

    func saveSnapshots(_ snapshots: [ReportSnapshot]) {
        savedSnapshots = snapshots
        storedSnapshots = snapshots
    }
}

private final class MockCorrelationAnalyzer: CorrelationAnalyzerProtocol {
    var receivedRanges: [Int] = []

    func analyze(insights: [ConversationInsight], rangeDays: Int, now: TimeInterval) -> ReportSnapshot {
        receivedRanges.append(rangeDays)
        return ReportSnapshot(
            rangeDays: rangeDays,
            generatedAt: now,
            sampleCount: insights.count,
            topSymptoms: [NamedCount(name: "失眠", count: 1)],
            topFactors: [NamedCount(name: "咖啡因", count: 1)],
            correlations: [SymptomFactorCorrelation(symptom: "失眠", factor: "咖啡因", count: 1)],
            specialReminder: nil
        )
    }
}

private struct FailingRemoteJournalAPIClient: RemoteJournalAPIClientProtocol {
    func fetchEntries(userID: String) throws -> [JournalEntryDTO] {
        throw RemoteAPIError.injectedFailure
    }

    func uploadEntries(_ entries: [JournalEntryDTO], userID: String) throws {
        throw RemoteAPIError.injectedFailure
    }
}

private struct StubRemoteJournalAPIClient: RemoteJournalAPIClientProtocol {
    var fetchResult: [JournalEntryDTO]

    func fetchEntries(userID: String) throws -> [JournalEntryDTO] {
        fetchResult
    }

    func uploadEntries(_ entries: [JournalEntryDTO], userID: String) throws {}
}

private struct StubAuthAPIClient: AuthAPIClientProtocol {
    var loginResult: Result<AuthTokenPayload, AuthAPIError>
    var refreshResult: Result<AuthTokenPayload, AuthAPIError>

    func sendPhoneCode(_ phone: String) throws {}

    func verifyPhoneCode(_ phone: String, code: String) throws -> AuthTokenPayload {
        try loginResult.get()
    }

    func loginWithPhone(_ phone: String) throws -> AuthTokenPayload {
        try loginResult.get()
    }

    func loginWithTestAccount(_ phone: String, secret: String) throws -> AuthTokenPayload {
        try loginResult.get()
    }

    func loginWithWeChatCode(_ code: String) throws -> AuthTokenPayload {
        try loginResult.get()
    }

    func refreshToken(_ refreshToken: String, userID: String) throws -> AuthTokenPayload {
        try refreshResult.get()
    }
}

private func makeJournalEntry(id: String, symptoms: [String], triggers: [String]) -> JournalEntry {
    JournalEntry(
        id: id,
        date: "2026-04-24",
        rawText: "mock text",
        createdAt: 0,
        extracted: ExtractedData(
            text: "mock text",
            foods: [],
            drinks: [],
            work: WorkData(busy: false, tired: false, level: 1),
            symptoms: symptoms,
            events: symptoms.map { SymptomEvent(time: "今日", symptom: $0) },
            triggers: triggers
        )
    )
}
