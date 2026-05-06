import SwiftUI

struct WelcomeView: View {
    @EnvironmentObject var vm: AppViewModel
    @State private var acceptedDisclaimers = false
    @State private var showPhoneAuthSheet = false
    @State private var phoneInput = ""
    @State private var phoneCodeInput = ""
    @State private var phoneAuthError: String?
    @State private var phoneCodeSent = false
    @State private var phoneAuthMode: PhoneAuthMode = .register
    @State private var showWechatAuthSheet = false
    @State private var wechatCodeInput = ""
    @State private var wechatAuthError: String?
    @State private var testAccountSecret = "local-test-account-secret-2026"

    var body: some View {
        VStack(spacing: 18) {
            IOSStatusBar()
            Spacer()

            if let authError = vm.authErrorMessage {
                FrostedCard {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundStyle(.orange)
                            Text(authError)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(CATheme.text)
                        }
                        Button("清除并重试") {
                            vm.clearAuthError()
                        }
                        .buttonStyle(.plain)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(CATheme.primaryAlt)
                    }
                }
                .padding(.horizontal, 20)
            }

            RoundedRectangle(cornerRadius: 24)
                .fill(.white.opacity(0.88))
                .frame(width: 86, height: 86)
                .overlay(Image(systemName: "sparkles").font(.system(size: 34)).foregroundStyle(CATheme.primaryAlt))
                .shadow(color: .black.opacity(0.08), radius: 16, x: 0, y: 8)

            Text("潮安").font(.system(size: 42, weight: .bold)).foregroundStyle(CATheme.text)
            Text("继续前，请先登录或注册")
                .caText(.body)
                .foregroundStyle(CATheme.subText)

            FrostedCard {
                VStack(spacing: 10) {
                    Text("创建账号以保存你的记录与报告").font(.system(size: 14, weight: .semibold)).foregroundStyle(CATheme.subText)
                    authButton("使用手机号注册", icon: "iphone") {
                        startPhoneAuth(.register)
                    }
                    authButton("使用微信号注册", icon: "message") {
                        startWechatAuth()
                    }
                    authButton("已有账号登录", icon: "person") {
                        startPhoneAuth(.login)
                    }
                    Toggle(isOn: $acceptedDisclaimers) {
                        Text("我已阅读并理解：潮安提供健康支持与科普，不提供医疗诊断，不替代医生诊疗。")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(CATheme.subText)
                    }
                    .tint(CATheme.primary)
                    .accessibilityIdentifier("welcomeDisclaimerToggle")
                    Button("登录后进入潮安") {
                        startPhoneAuth(.login)
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .opacity(acceptedDisclaimers ? 1 : 0.45)
                    .disabled(!acceptedDisclaimers || vm.isAuthLoading)
                    Button("先进入（跳过登录）") {
                        vm.skipLoginForNow()
                    }
                    .buttonStyle(.plain)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(CATheme.primaryAlt)
                    .opacity(acceptedDisclaimers ? 1 : 0.45)
                    .disabled(!acceptedDisclaimers || vm.isAuthLoading)
                    if vm.isAuthLoading {
                        HStack(spacing: 8) {
                            ProgressView()
                            Text("正在登录…")
                                .font(.system(size: 12))
                                .foregroundStyle(CATheme.subText)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)

            Text("潮安提供的是健康支持与自我管理工具，不替代医生诊疗。若有严重不适，请及时就医。")
                .caText(.footnote)
                .foregroundStyle(CATheme.subText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)
            Spacer()
        }
        .sheet(isPresented: $showPhoneAuthSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    Text(phoneAuthMode.sheetSubtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(CATheme.subText)

                    TextField("请输入11位大陆手机号", text: $phoneInput)
                        .keyboardType(.numberPad)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 12)
                        .frame(height: 44)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.9))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(CATheme.border, lineWidth: 1)
                        )

                    TextField("请输入验证码", text: $phoneCodeInput)
                        .keyboardType(.numberPad)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 12)
                        .frame(height: 44)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.9))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(CATheme.border, lineWidth: 1)
                        )

                    if let phoneAuthError, !phoneAuthError.isEmpty {
                        Text(phoneAuthError)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.red)
                    }

                    Button(phoneCodeSent ? "重新发送验证码" : "发送验证码") {
                        sendPhoneCode()
                    }
                    .buttonStyle(.plain)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(CATheme.primaryAlt)
                    .disabled(vm.isAuthLoading)

                    Button(phoneAuthMode.confirmTitle) {
                        submitPhoneAuth()
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(vm.isAuthLoading || !phoneCodeSent)

                    if phoneAuthMode == .login {
                        Button("测试账号快速登录（跳过验证码）") {
                            submitTestAccountLogin()
                        }
                        .buttonStyle(.plain)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(CATheme.primaryAlt)
                        .disabled(vm.isAuthLoading)
                    }

                    if vm.isAuthLoading {
                        HStack(spacing: 8) {
                            ProgressView()
                            Text("正在处理…")
                                .font(.system(size: 12))
                                .foregroundStyle(CATheme.subText)
                        }
                    }

                    Spacer()
                }
                .padding(16)
                .navigationTitle(phoneAuthMode.sheetTitle)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("取消") {
                            showPhoneAuthSheet = false
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showWechatAuthSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    Text("请粘贴微信开放平台回调得到的授权码 code。")
                        .font(.system(size: 13))
                        .foregroundStyle(CATheme.subText)

                    TextField("请输入微信授权码", text: $wechatCodeInput)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 12)
                        .frame(height: 44)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.9))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(CATheme.border, lineWidth: 1)
                        )

                    if let wechatAuthError, !wechatAuthError.isEmpty {
                        Text(wechatAuthError)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.red)
                    }

                    Button("微信注册/登录并进入") {
                        submitWechatAuth()
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(vm.isAuthLoading)

                    if vm.isAuthLoading {
                        HStack(spacing: 8) {
                            ProgressView()
                            Text("正在处理…")
                                .font(.system(size: 12))
                                .foregroundStyle(CATheme.subText)
                        }
                    }

                    Spacer()
                }
                .padding(16)
                .navigationTitle("微信注册/登录")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("取消") {
                            showWechatAuthSheet = false
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func authButton(_ title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button {
            action()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: icon)
                Text(title)
            }
            .font(.system(size: 14, weight: .semibold))
            .caText(.body)
            .foregroundStyle(CATheme.subText)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Capsule().fill(.white.opacity(0.76)))
            .overlay(Capsule().stroke(CATheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .opacity(acceptedDisclaimers ? 1 : 0.45)
        .disabled(vm.isAuthLoading || !acceptedDisclaimers)
    }

    private func startPhoneAuth(_ mode: PhoneAuthMode) {
        vm.clearAuthError()
        phoneAuthMode = mode
        phoneAuthError = nil
        phoneInput = ""
        phoneCodeInput = ""
        phoneCodeSent = false
        showPhoneAuthSheet = true
    }

    private func sendPhoneCode() {
        let digits = phoneInput.filter(\.isNumber)
        guard digits.count == 11, digits.hasPrefix("1") else {
            phoneAuthError = "请输入正确的11位大陆手机号。"
            return
        }
        vm.clearAuthError()
        let ok = vm.sendPhoneCode(digits)
        if !ok, let authError = vm.authErrorMessage, !authError.isEmpty {
            phoneAuthError = authError
            return
        }
        phoneAuthError = nil
        phoneCodeSent = true
    }

    private func submitPhoneAuth() {
        let digits = phoneInput.filter(\.isNumber)
        guard digits.count == 11, digits.hasPrefix("1") else {
            phoneAuthError = "请输入正确的11位大陆手机号。"
            return
        }
        let code = phoneCodeInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !code.isEmpty else {
            phoneAuthError = "请输入验证码。"
            return
        }
        vm.clearAuthError()
        vm.loginWithPhoneCode(digits, code: code)
        if let authError = vm.authErrorMessage, !authError.isEmpty {
            phoneAuthError = authError
            return
        }
        phoneAuthError = nil
        showPhoneAuthSheet = false
    }

    private func submitTestAccountLogin() {
        let digits = phoneInput.filter(\.isNumber)
        guard digits.count == 11, digits.hasPrefix("1") else {
            phoneAuthError = "请输入正确的11位大陆手机号。"
            return
        }
        vm.clearAuthError()
        vm.loginWithTestAccount(digits, secret: testAccountSecret)
        if let authError = vm.authErrorMessage, !authError.isEmpty {
            phoneAuthError = authError
            return
        }
        phoneAuthError = nil
        showPhoneAuthSheet = false
    }

    private func startWechatAuth() {
        vm.clearAuthError()
        wechatCodeInput = ""
        wechatAuthError = nil
        showWechatAuthSheet = true
    }

    private func submitWechatAuth() {
        let code = wechatCodeInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !code.isEmpty else {
            wechatAuthError = "请输入微信授权码。"
            return
        }
        vm.clearAuthError()
        vm.loginWithWeChatCode(code)
        if let authError = vm.authErrorMessage, !authError.isEmpty {
            wechatAuthError = authError
            return
        }
        wechatAuthError = nil
        showWechatAuthSheet = false
    }
}

private enum PhoneAuthMode {
    case register
    case login

    var sheetTitle: String {
        switch self {
        case .register: return "手机号注册"
        case .login: return "账号登录"
        }
    }

    var sheetSubtitle: String {
        switch self {
        case .register: return "输入手机号后将创建账号并进入潮安。"
        case .login: return "输入已注册手机号后登录。"
        }
    }

    var confirmTitle: String {
        switch self {
        case .register: return "注册并进入"
        case .login: return "登录并进入"
        }
    }
}
