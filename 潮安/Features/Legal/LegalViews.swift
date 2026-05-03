import SwiftUI

struct PrivacyPolicyView: View {
    var body: some View {
        LegalDocumentScaffold(
            title: "隐私政策",
            sections: [
                "我们仅收集提供健康记录服务所需的最小化信息。",
                "敏感字段默认脱敏处理，未经授权不对外共享。",
                "你可在设置页申请导出或删除个人数据。"
            ]
        )
    }
}

struct UserAgreementView: View {
    var body: some View {
        LegalDocumentScaffold(
            title: "用户协议",
            sections: [
                "本应用用于健康记录与一般健康管理建议，不构成医疗诊断。",
                "请对账号与设备访问进行妥善管理，避免他人未经授权使用。",
                "如发现异常内容或服务问题，可通过官方渠道反馈。"
            ]
        )
    }
}

struct MedicalDisclaimerView: View {
    var body: some View {
        LegalDocumentScaffold(
            title: "医疗免责声明",
            sections: [
                "AI 建议仅作为健康管理参考，不替代医生面诊。",
                "出现胸痛、呼吸困难、晕厥、自伤念头等高危情况请立即就医。",
                "若症状持续或加重，请优先寻求专业医疗帮助。"
            ]
        )
    }
}

private struct LegalDocumentScaffold: View {
    let title: String
    let sections: [String]

    var body: some View {
        PageScaffold(title: title, showBack: true) {
            FrostedCard {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(sections.enumerated()), id: \.offset) { idx, text in
                        Text("\(idx + 1). \(text)")
                            .font(.system(size: 14))
                            .foregroundStyle(CATheme.text)
                    }
                }
            }
        }
    }
}
