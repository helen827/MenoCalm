import SwiftUI

enum CATheme {
    static let bg = Color(hex: 0xF7F3F3)
    static let primary = Color(hex: 0xE4B0B1)
    static let primaryAlt = Color(hex: 0xE4B1A1)
    static let blue = Color(hex: 0xB6C8DB)
    static let lilac = Color(hex: 0xE9E1E3)
    static let card = Color.white.opacity(0.58)
    static let text = Color(hex: 0x2F2B2C)
    static let subText = Color(hex: 0x6F6A6C)
    static let border = Color(hex: 0xDDD5D8).opacity(0.65)
}

enum CASpacing {
    static let xs: CGFloat = 8
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let lg: CGFloat = 20
    static let xl: CGFloat = 24
}

enum CATypography {
    case pageTitle
    case sectionTitle
    case body
    case caption
    case footnote

    var font: Font {
        switch self {
        case .pageTitle:
            return .system(size: 20, weight: .bold)
        case .sectionTitle:
            return .system(size: 16, weight: .semibold)
        case .body:
            return .system(size: 14, weight: .regular)
        case .caption:
            return .system(size: 12, weight: .medium)
        case .footnote:
            return .system(size: 11, weight: .regular)
        }
    }
}

extension View {
    func caText(_ style: CATypography) -> some View {
        self.font(style.font)
    }
}

extension Color {
    init(hex: UInt) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: 1
        )
    }
}
