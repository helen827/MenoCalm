import SwiftUI
#if canImport(SwiftData)
import SwiftData
#endif

@main
struct __App: App {
#if canImport(SwiftData)
    private let sharedModelContainer: ModelContainer = {
        do {
            let schema = Schema(LocalSchemaV1.allModels)
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to create SwiftData ModelContainer: \(error)")
        }
    }()
#endif

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
#if canImport(SwiftData)
        .modelContainer(sharedModelContainer)
#endif
    }
}
