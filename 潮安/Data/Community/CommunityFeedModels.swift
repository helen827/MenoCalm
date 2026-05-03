import CoreGraphics
import Foundation

struct CommunityFeedItem: Codable, Identifiable, Hashable {
    var id: String
    var title: String
    var tag: String
    var cardHeight: Double
    var isOfficial: Bool

    var heightPoints: CGFloat { CGFloat(cardHeight) }
}

struct CommunityFeedFile: Codable {
    var official: [CommunityFeedItem]
    var user: [CommunityFeedItem]
}

enum CommunityFeedSource: String {
    case bundled
    case remote
}

struct CommunityFeedPayload {
    var official: [CommunityFeedItem]
    var user: [CommunityFeedItem]
    var source: CommunityFeedSource
}
