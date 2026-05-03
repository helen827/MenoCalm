import Foundation

enum AppRuntimeEnvironment: String, CaseIterable {
    case dev
    case staging
    case prod
}

struct BackendServiceConfig {
    var baseURL: URL?
    var timeout: TimeInterval
}

struct AppRuntimeConfig {
    var environment: AppRuntimeEnvironment
    var backend: BackendServiceConfig
}

enum RuntimeConfigError: Error, Equatable {
    case invalidEnvironment(String)
    case invalidBackendURL(String)
    case productionEndpointMissing
    case insecureProductionEndpoint
}

struct RuntimeConfigResolver {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func resolve() throws -> AppRuntimeConfig {
        let envRaw = defaults.string(forKey: "chaoan_runtime_env") ?? AppRuntimeEnvironment.dev.rawValue
        guard let environment = AppRuntimeEnvironment(rawValue: envRaw) else {
            throw RuntimeConfigError.invalidEnvironment(envRaw)
        }

        let legacyBase = defaults.string(forKey: "chaoan_backend_base_url")
        let envSpecific = defaults.string(forKey: "chaoan_backend_base_url_\(environment.rawValue)")
        let selectedBase = (envSpecific?.isEmpty == false ? envSpecific : legacyBase) ?? defaultBaseURLString(for: environment)
        let baseURL = try parseURL(selectedBase)

        if environment == .prod {
            guard let baseURL else {
                throw RuntimeConfigError.productionEndpointMissing
            }
            if baseURL.host == "localhost" || baseURL.scheme?.lowercased() != "https" {
                throw RuntimeConfigError.insecureProductionEndpoint
            }
        }

        let timeout = defaults.double(forKey: "chaoan_backend_timeout_seconds")
        let requestTimeout = timeout > 0 ? timeout : 8

        return AppRuntimeConfig(
            environment: environment,
            backend: BackendServiceConfig(
                baseURL: baseURL,
                timeout: requestTimeout
            )
        )
    }

    func resolveOrFallback() -> AppRuntimeConfig {
        (try? resolve()) ?? AppRuntimeConfig(
            environment: .dev,
            backend: BackendServiceConfig(baseURL: nil, timeout: 8)
        )
    }

    private func parseURL(_ raw: String?) throws -> URL? {
        guard let raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        guard let url = URL(string: raw) else {
            throw RuntimeConfigError.invalidBackendURL(raw)
        }
        return url
    }

    private func defaultBaseURLString(for env: AppRuntimeEnvironment) -> String? {
        switch env {
        case .dev:
            return nil
        case .staging:
            return "https://staging-api.menocalm.example"
        case .prod:
            return nil
        }
    }
}
