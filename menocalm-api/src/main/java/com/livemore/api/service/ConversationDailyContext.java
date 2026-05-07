package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationMessageDto;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * For daily-tracking chat: restrict LLM context to all messages from the same calendar day
 * as the latest message (by {@code createdAtMs}), in the given zone (e.g. Asia/Shanghai).
 */
public final class ConversationDailyContext {

    private ConversationDailyContext() {
    }

    public static ZoneId parseZoneId(String raw) {
        if (raw == null || raw.isBlank()) {
            return ZoneId.of("Asia/Shanghai");
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (DateTimeException ex) {
            return ZoneId.of("Asia/Shanghai");
        }
    }

    public static List<ConversationMessageDto> sameCalendarDayAsLatest(
            List<ConversationMessageDto> messages,
            ZoneId zone
    ) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        long latestMs = Long.MIN_VALUE;
        for (ConversationMessageDto m : messages) {
            if (m == null || m.getCreatedAtMs() == null) {
                continue;
            }
            latestMs = Math.max(latestMs, m.getCreatedAtMs());
        }
        if (latestMs == Long.MIN_VALUE) {
            return List.copyOf(messages);
        }
        LocalDate day = Instant.ofEpochMilli(latestMs).atZone(zone).toLocalDate();
        long startInclusive = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long endExclusive = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        List<ConversationMessageDto> out = new ArrayList<>();
        for (ConversationMessageDto m : messages) {
            if (m == null || m.getCreatedAtMs() == null) {
                continue;
            }
            long t = m.getCreatedAtMs();
            if (t >= startInclusive && t < endExclusive) {
                out.add(m);
            }
        }
        return out;
    }
}
