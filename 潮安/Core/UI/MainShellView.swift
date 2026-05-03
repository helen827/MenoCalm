import SwiftUI

struct MainShellView: View {
    @EnvironmentObject var router: AppRouter

    var body: some View {
        VStack(spacing: 0) {
            IOSStatusBar()
            NavigationStack(path: $router.path) {
                Group {
                    switch router.selectedTab {
                    case .home:
                        HomeView()
                    case .learn:
                        LearnView()
                    case .practice:
                        PracticeView()
                    case .profile:
                        ProfileView()
                    }
                }
                .navigationBarBackButtonHidden(true)
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .articleDetail:
                        ArticleDetailView()
                    case .trendReport:
                        TrendReportView()
                    case .medicalList:
                        MedicalListView()
                    case .settings:
                        SettingsView()
                    case .privacyPolicy:
                        PrivacyPolicyView()
                    case .userAgreement:
                        UserAgreementView()
                    case .medicalDisclaimer:
                        MedicalDisclaimerView()
                    case .practiceDetail:
                        PracticeDetailView()
                    case .communityPost:
                        CommunityPostView()
                    case .communityPostDetail:
                        CommunityPostDetailView()
                    case .userStatus:
                        UserStatusView()
                    case .basicInfo:
                        BasicInfoView()
                    case .selfTest:
                        SelfTestView()
                    case .selfTestResult:
                        SelfTestResultView()
                    }
                }
            }
            CATabBar()
        }
    }
}

struct CATabBar: View {
    @EnvironmentObject var router: AppRouter

    var body: some View {
        HStack {
            ForEach(MainTab.allCases, id: \.self) { tab in
                Button {
                    router.resetToTab(tab)
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 17, weight: .medium))
                        Text(tab.rawValue)
                            .caText(.footnote)
                    }
                    .foregroundStyle(router.selectedTab == tab ? CATheme.text : CATheme.subText)
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, CASpacing.sm)
        .padding(.vertical, 10)
        .background(.white.opacity(0.75))
        .overlay(Rectangle().fill(CATheme.border).frame(height: 1), alignment: .top)
    }
}
