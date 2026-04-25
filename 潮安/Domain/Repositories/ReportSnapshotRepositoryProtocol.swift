import Foundation

protocol ReportSnapshotRepositoryProtocol {
    func loadSnapshots() -> [ReportSnapshot]
    func saveSnapshots(_ snapshots: [ReportSnapshot])
}
