package com.livemore.api.service;

import com.livemore.api.web.dto.SymptomLogPayloadDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic extraction for symptoms_log until a dedicated LLM extraction path exists.
 * Output shape is stable for downstream SQL/warehouse analytics.
 */
@Component
public class RuleBasedSymptomLogExtractor {

    private static final Pattern SEVERITY_SLASH = Pattern.compile("(\\d{1,2})\\s*/\\s*10");
    private static final Pattern SEVERITY_CN = Pattern.compile("(\\d{1,2})\\s*分");

    public SymptomLogPayloadDto extract(String rawContent) {
        String text = rawContent == null ? "" : rawContent.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        SymptomLogPayloadDto dto = new SymptomLogPayloadDto();

        Set<String> symptoms = new LinkedHashSet<>();
        if (containsAny(lower, "潮热", "发热感", "hot flash")) {
            symptoms.add("潮热");
        }
        if (containsAny(lower, "盗汗", "夜间出汗")) {
            symptoms.add("盗汗");
        }
        if (containsAny(lower, "失眠", "睡不着", "早醒", "夜醒")) {
            symptoms.add("失眠");
        }
        if (containsAny(lower, "心悸", "心慌", "心跳快", "palpitation")) {
            symptoms.add("心悸");
        }
        if (containsAny(lower, "头痛", "头疼")) {
            symptoms.add("头痛");
        }
        if (containsAny(lower, "关节", "肌肉酸痛", "浑身疼")) {
            symptoms.add("关节肌肉不适");
        }
        if (containsAny(lower, "皮肤干", "瘙痒", "发痒")) {
            symptoms.add("皮肤干痒");
        }
        if (containsAny(lower, "健忘", "脑子糊", "注意力不集中", "brain fog")) {
            symptoms.add("脑雾/注意力");
        }
        if (containsAny(lower, "焦虑", "紧张", "panic", "anxiety")) {
            symptoms.add("焦虑情绪");
        }
        if (containsAny(lower, "抑郁", "低落", "depress")) {
            symptoms.add("情绪低落");
        }
        if (containsAny(lower, "烦躁", "易怒", "脾气")) {
            symptoms.add("烦躁易怒");
        }
        if (containsAny(lower, "阴道干", "性交痛", "性欲")) {
            symptoms.add("生殖泌尿不适");
        }
        if (containsAny(lower, "尿频", "尿急", "尿失禁", "漏尿")) {
            symptoms.add("泌尿症状");
        }
        dto.getSymptomDetected().addAll(symptoms);

        Set<String> lifestyle = new LinkedHashSet<>();
        if (containsAny(lower, "咖啡", "咖啡因", "caffeine")) {
            lifestyle.add("咖啡因");
        }
        if (containsAny(lower, "酒", "酒精", "alcohol")) {
            lifestyle.add("酒精");
        }
        if (containsAny(lower, "熬夜", "晚睡", "睡眠不足", "失眠")) {
            lifestyle.add("睡眠节律");
        }
        if (containsAny(lower, "运动", "锻炼", "exercise")) {
            lifestyle.add("运动");
        }
        if (containsAny(lower, "压力", "加班", "stress")) {
            lifestyle.add("压力");
        }
        if (containsAny(lower, "辛辣", "辣", "spicy")) {
            lifestyle.add("辛辣饮食");
        }
        dto.getLifestyleFactors().addAll(lifestyle);

        if (containsAny(lower, "焦虑", "紧张")) {
            dto.setEmotionalStatus("焦虑/紧张");
        } else if (containsAny(lower, "抑郁", "低落", "想哭")) {
            dto.setEmotionalStatus("低落/抑郁倾向");
        } else if (containsAny(lower, "烦躁", "易怒")) {
            dto.setEmotionalStatus("烦躁易怒");
        } else if (containsAny(lower, "平静", "还好", "一般")) {
            dto.setEmotionalStatus("相对平稳");
        } else {
            dto.setEmotionalStatus("未标注");
        }

        dto.setSeverity(parseSeverity(text));

        dto.setRelevantToMenopause(!symptoms.isEmpty()
                || containsAny(lower, "更年期", "绝经", "围绝经期", "雌激素", "menopause", "perimenopause"));

        return dto;
    }

    private static String parseSeverity(String text) {
        Matcher m1 = SEVERITY_SLASH.matcher(text);
        if (m1.find()) {
            return clampTen(m1.group(1));
        }
        Matcher m2 = SEVERITY_CN.matcher(text);
        if (m2.find()) {
            return clampTen(m2.group(1));
        }
        return null;
    }

    private static String clampTen(String digits) {
        try {
            int v = Integer.parseInt(digits);
            if (v < 1) {
                return "1";
            }
            if (v > 10) {
                return "10";
            }
            return Integer.toString(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (n != null && text.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
