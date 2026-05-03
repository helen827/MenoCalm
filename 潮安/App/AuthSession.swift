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
        currentUserID == "guest-local"
    }

    func loginWithPhone(_ phone: String) {
        let digits = phone.filter(\.isNumber)
        guard !digits.isEmpty else { return }
        login(userID: "phone_\(digits)", phoneNumber: digits)
    }

    func login(userID: String, phoneNumber: String? = nil) {
        self.phoneNumber = phoneNumber
        currentUserID = userID
    }

    func logout() {
        phoneNumber = nil
        currentUserID = "guest-local"
    }
}
