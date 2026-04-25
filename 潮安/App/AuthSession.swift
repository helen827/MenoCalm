import Foundation
import Combine

final class AuthSession: ObservableObject {
    @Published private(set) var currentUserID: String
    @Published private(set) var phoneNumber: String?

    init(currentUserID: String = "guest-local", phoneNumber: String? = nil) {
        self.currentUserID = currentUserID
        self.phoneNumber = phoneNumber
    }

    var isAnonymous: Bool {
        phoneNumber == nil
    }

    func loginWithPhone(_ phone: String) {
        let digits = phone.filter(\.isNumber)
        guard !digits.isEmpty else { return }
        phoneNumber = digits
        currentUserID = "phone_\(digits)"
    }
}
