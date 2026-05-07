package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationMessageDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationDailyContextTest {

    @Test
    void parseZoneId_invalidFallsBackToShanghai() {
        assertEquals(ZoneId.of("Asia/Shanghai"), ConversationDailyContext.parseZoneId("not-a-zone"));
    }

    @Test
    void sameCalendarDayAsLatest_keepsOnlyDayOfNewestMessage_utc() {
        ZoneId utc = ZoneId.of("UTC");
        long day1 = Instant.parse("2026-05-01T12:00:00Z").toEpochMilli();
        long day2a = Instant.parse("2026-05-02T08:00:00Z").toEpochMilli();
        long day2b = Instant.parse("2026-05-02T18:00:00Z").toEpochMilli();

        ConversationMessageDto m1 = new ConversationMessageDto();
        m1.setId("a");
        m1.setRole("user");
        m1.setContent("older day");
        m1.setCreatedAtMs(day1);

        ConversationMessageDto m2 = new ConversationMessageDto();
        m2.setId("b");
        m2.setRole("user");
        m2.setContent("morning");
        m2.setCreatedAtMs(day2a);

        ConversationMessageDto m3 = new ConversationMessageDto();
        m3.setId("c");
        m3.setRole("assistant");
        m3.setContent("reply");
        m3.setCreatedAtMs(day2b);

        List<ConversationMessageDto> out = ConversationDailyContext.sameCalendarDayAsLatest(
                List.of(m1, m2, m3),
                utc
        );
        assertEquals(List.of(m2, m3), out);
    }
}
