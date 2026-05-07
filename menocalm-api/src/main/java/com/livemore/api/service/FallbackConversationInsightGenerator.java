package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class FallbackConversationInsightGenerator {

    private static final String MEDICAL_DISCLAIMER =
            "以上内容仅为科普参考，不作为医疗诊断建议。如症状严重请及时就医。";

    public ConversationInsightDto generate(List<ConversationMessageDto> messages) {
        String lastUser = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessageDto m = messages.get(i);
            if (m != null && "user".equalsIgnoreCase(m.getRole())) {
                lastUser = m.getContent() == null ? "" : m.getContent().trim();
                break;
            }
        }
        String normalized = lastUser.toLowerCase(Locale.ROOT);
        Set<String> tags = new LinkedHashSet<>();
        List<String> suggestions = new ArrayList<>();

        if (containsAny(normalized, "潮热", "出汗", "盗汗", "hot flash")) {
            tags.add("潮热");
            tags.add("出汗");
            suggestions.add("发作时先降温：风扇/冷敷颈后/分层穿衣，通常几分钟内缓解");
            suggestions.add("连续7天记录发作时间、强度、触发因素（咖啡/辛辣/情绪/室温）");
        }
        if (containsAny(normalized, "失眠", "睡不着", "夜醒", "sleep")) {
            tags.add("失眠");
            suggestions.add("固定起床时间，睡前90分钟减少刷屏/咖啡因/酒精");
            suggestions.add("睡前做10分钟慢呼吸或放松练习，先把身体从高唤醒拉下来");
        }
        if (containsAny(normalized, "焦虑", "烦躁", "心慌", "心悸", "panic", "anx")) {
            tags.add("情绪波动");
            suggestions.add("先做3轮慢呼吸（吸4秒、呼6秒），让心率先降下来");
            suggestions.add("写下“触发事件-当下想法-身体反应”，减少反复内耗");
        }

        String risk = "low";
        if (containsAny(normalized, "胸痛", "呼吸困难", "晕厥", "自杀", "伤害自己", "大出血", "血尿", "发热", "腰痛")) {
            risk = "high";
            tags.add("高风险信号");
            suggestions.add(0, "若出现胸痛/呼吸困难/晕厥/强烈自伤冲动等，请立即就医或拨打急救电话");
        } else if (containsAny(normalized, "严重", "影响生活", "几乎每天", "加重")) {
            risk = "medium";
        }

        if (suggestions.isEmpty()) {
            suggestions = new ArrayList<>(List.of(
                    "连续7天记录症状出现时间、强度和触发因素",
                    "优先调整睡眠节律、饮食刺激和压力管理",
                    "若症状持续加重或影响生活，尽快到妇科/更年期门诊评估"
            ));
        }

        ConversationInsightDto dto = new ConversationInsightDto();
        dto.setSummary(buildSummary(lastUser, risk));
        dto.setRiskLevel(risk);
        dto.setSymptomTags(List.copyOf(tags.isEmpty() ? List.of("围绝经期常见困扰") : tags));
        dto.setSuggestions(finalizeSuggestions(suggestions));
        dto.setSource("fallback");
        dto.setDegradedReason("ai_provider_unavailable");
        return dto;
    }

    /**
     * Cap actionable tips, then append the fixed medical disclaimer as the final item.
     */
    private List<String> finalizeSuggestions(List<String> suggestions) {
        List<String> action = new ArrayList<>(suggestions);
        action.removeIf(s -> s != null && MEDICAL_DISCLAIMER.equals(s.trim()));
        while (action.size() > 5) {
            action.remove(action.size() - 1);
        }
        action.add(MEDICAL_DISCLAIMER);
        return List.copyOf(action);
    }

    private String buildSummary(String lastUser, String risk) {
        String base = "我理解你在描述围绝经期/更年期常见的不适表现。";
        if (lastUser != null && !lastUser.isBlank()) {
            base = "我理解你提到的情况：" + trimTo(lastUser, 34) + "。";
        }
        if ("high".equalsIgnoreCase(risk)) {
            return base + " 其中包含高风险信号，需要优先线下评估。";
        }
        if ("medium".equalsIgnoreCase(risk)) {
            return base + " 目前风险提示中等，建议尽早做针对性调整与评估。";
        }
        return base + " 目前风险提示偏低，可先按步骤自我管理并持续观察。";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String k : keywords) {
            if (k != null && !k.isBlank() && text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private String trimTo(String text, int max) {
        String t = text.trim().replace('\n', ' ').replace('\r', ' ');
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }
}
