import SwiftUI

struct ArticleDetailView: View {
    var body: some View {
        PageScaffold(title: "科普详情", showBack: true) {
            FrostedCard {
                VStack(alignment: .leading, spacing: 10) {
                    Text("更年期睡眠管理：四个晚间习惯")
                        .font(.system(size: 20, weight: .bold))
                    HStack(spacing: 6) {
                        Text("官方科普")
                        Text("今天发布")
                    }
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(CATheme.subText)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(.white.opacity(0.72)))

                    Text("1）固定作息时间\n2）下午减少咖啡因\n3）睡前降低屏幕刺激\n4）可尝试 4-7-8 呼吸放松。")
                        .font(.system(size: 14))
                        .foregroundStyle(CATheme.text)
                        .lineSpacing(3)

                    Text("说明：本文为健康科普，不构成医学诊断。若症状持续或加重，请咨询医生。")
                        .font(.system(size: 12))
                        .foregroundStyle(CATheme.subText)
                }
            }
        }
    }
}
