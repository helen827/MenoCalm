import Foundation

struct RepositoryFacade: JournalRepositoryProtocol {
    private let localRepository: JournalRepositoryProtocol
    private let remoteRepository: RemoteJournalRepository
    private let syncQueue: SyncQueue
    private let featureFlags: FeatureFlags
    private let rolloutMonitor: RolloutMonitor
    private let conflictResolver: JournalConflictResolver

    init(
        localRepository: JournalRepositoryProtocol,
        remoteRepository: RemoteJournalRepository,
        syncQueue: SyncQueue,
        featureFlags: FeatureFlags,
        rolloutMonitor: RolloutMonitor = RolloutMonitor(),
        conflictResolver: JournalConflictResolver = JournalConflictResolver()
    ) {
        self.localRepository = localRepository
        self.remoteRepository = remoteRepository
        self.syncQueue = syncQueue
        self.featureFlags = featureFlags
        self.rolloutMonitor = rolloutMonitor
        self.conflictResolver = conflictResolver
    }

    func loadEntries() -> [JournalEntry] {
        let local = localRepository.loadEntries()
        rolloutMonitor.markLocalRead()

        guard featureFlags.cloudReadEnabled else {
            return local
        }

        do {
            let remote = try remoteRepository.fetchOrThrow()
            rolloutMonitor.markRemoteRead()
            let merged = conflictResolver.resolve(local: local, remote: remote)
            localRepository.saveEntries(merged)
            return merged
        } catch {
            rolloutMonitor.markRemoteFailure()
            if featureFlags.failOpenToLocalData {
                return local
            }
            return []
        }
    }

    func saveEntries(_ entries: [JournalEntry]) {
        localRepository.saveEntries(entries)
        guard featureFlags.cloudSyncEnabled else { return }

        syncQueue.enqueueUpload(entries: entries)
        syncQueue.flush { payload in
            do {
                try remoteRepository.uploadOrThrow(payload)
                rolloutMonitor.markSyncSuccess()
            } catch {
                rolloutMonitor.markSyncFailure()
                throw error
            }
        }
    }

}
