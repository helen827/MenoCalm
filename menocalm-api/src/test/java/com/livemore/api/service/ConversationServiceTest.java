package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationStore conversationStore;
    @Mock
    private ConversationAnalysisClient analysisClient;
    @Mock
    private MenopauseAnalyticsWriter analyticsWriter;

    private ConversationService service() {
        return new ConversationService(
                conversationStore,
                analysisClient,
                new FallbackConversationInsightGenerator(),
                analyticsWriter,
                "UTC"
        );
    }

    @Test
    void appendMessage_userMismatch_returns403() {
        ConversationService service = service();
        ConversationMessageDto dto = new ConversationMessageDto();
        dto.setConversationId("c1");
        dto.setRole("user");
        dto.setContent("hello");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.appendMessage("u1", "u2", dto)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void appendMessage_fillsDefaultsAndPersists() {
        ConversationService service = service();
        ConversationMessageDto dto = new ConversationMessageDto();
        dto.setConversationId("c1");
        dto.setRole("USER");
        dto.setContent("hello");

        ConversationMessageDto saved = service.appendMessage("u1", "u1", dto);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAtMs());
        assertEquals("user", saved.getRole());
        ArgumentCaptor<ConversationMessageDto> captor = ArgumentCaptor.forClass(ConversationMessageDto.class);
        verify(conversationStore).appendMessage(eq("u1"), captor.capture());
        assertEquals("c1", captor.getValue().getConversationId());
        verify(analyticsWriter).recordAfterUserMessage(eq("u1"), any());
    }

    @Test
    void analyzeAndUpsertInsight_emptyConversation_returns400() {
        ConversationService service = service();
        when(conversationStore.listMessages("u1", "c1", 200)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.analyzeAndUpsertInsight("u1", "u1", "c1")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void analyzeAndUpsertInsight_passesOnlySameUtcDayAsLatestMessage() {
        ConversationService service = service();
        ConversationMessageDto older = new ConversationMessageDto();
        older.setId("m0");
        older.setConversationId("c1");
        older.setRole("user");
        older.setContent("yesterday");
        older.setCreatedAtMs(Instant.parse("2026-05-01T10:00:00Z").toEpochMilli());

        ConversationMessageDto today = new ConversationMessageDto();
        today.setId("m1");
        today.setConversationId("c1");
        today.setRole("user");
        today.setContent("today");
        today.setCreatedAtMs(Instant.parse("2026-05-02T09:00:00Z").toEpochMilli());

        when(conversationStore.listMessages("u1", "c1", 200)).thenReturn(List.of(older, today));
        ConversationInsightDto analyzed = new ConversationInsightDto();
        analyzed.setSummary("ok");
        when(analysisClient.analyze(List.of(today))).thenReturn(analyzed);

        ConversationInsightDto result = service.analyzeAndUpsertInsight("u1", "u1", "c1");

        assertEquals("c1", result.getConversationId());
        verify(analysisClient).analyze(List.of(today));
        verifyNoMoreInteractions(analysisClient);
        verify(conversationStore).upsertInsight(eq("u1"), eq("c1"), eq(result));
        verify(analyticsWriter).recordInsightSnapshot(eq("u1"), eq("c1"), eq(result));
    }

    @Test
    void analyzeAndUpsertInsight_callsClientAndStores() {
        ConversationService service = service();
        ConversationMessageDto msg = new ConversationMessageDto();
        msg.setId("m1");
        msg.setConversationId("c1");
        msg.setRole("user");
        msg.setContent("最近总失眠");
        msg.setCreatedAtMs(System.currentTimeMillis());
        when(conversationStore.listMessages("u1", "c1", 200)).thenReturn(List.of(msg));
        ConversationInsightDto analyzed = new ConversationInsightDto();
        analyzed.setSummary("可能与更年期相关");
        when(analysisClient.analyze(List.of(msg))).thenReturn(analyzed);

        ConversationInsightDto result = service.analyzeAndUpsertInsight("u1", "u1", "c1");

        assertEquals("c1", result.getConversationId());
        verify(conversationStore).upsertInsight(eq("u1"), eq("c1"), eq(result));
        verify(analyticsWriter).recordInsightSnapshot(eq("u1"), eq("c1"), eq(result));
    }

    @Test
    void analyzeAndUpsertInsight_ai503_noCachedInsight_returnsFallback() {
        ConversationService service = service();
        ConversationMessageDto msg = new ConversationMessageDto();
        msg.setId("m1");
        msg.setConversationId("c1");
        msg.setRole("user");
        msg.setContent("最近总失眠");
        msg.setCreatedAtMs(System.currentTimeMillis());
        when(conversationStore.listMessages("u1", "c1", 200)).thenReturn(List.of(msg));
        when(analysisClient.analyze(List.of(msg))).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_failed"));

        ConversationInsightDto result = service.analyzeAndUpsertInsight("u1", "u1", "c1");

        assertEquals("c1", result.getConversationId());
        assertEquals("fallback", result.getSource());
        assertNotNull(result.getDegradedReason());
        verify(conversationStore).upsertInsight(eq("u1"), eq("c1"), eq(result));
        verify(analyticsWriter).recordInsightSnapshot(eq("u1"), eq("c1"), eq(result));
    }

    @Test
    void analyzeAndUpsertInsight_ai503_ignoresStaleCachedInsight() {
        ConversationService service = service();
        ConversationMessageDto msg = new ConversationMessageDto();
        msg.setId("m2");
        msg.setConversationId("c1");
        msg.setRole("user");
        msg.setContent("最近总失眠");
        msg.setCreatedAtMs(System.currentTimeMillis());
        when(conversationStore.listMessages("u1", "c1", 200)).thenReturn(List.of(msg));
        when(analysisClient.analyze(List.of(msg))).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_failed"));

        ConversationInsightDto result = service.analyzeAndUpsertInsight("u1", "u1", "c1");

        assertEquals("fallback", result.getSource());
        assertEquals("c1", result.getConversationId());
        assertTrue(
                (result.getSummary() != null && result.getSummary().contains("失眠"))
                        || (result.getSymptomTags() != null && result.getSymptomTags().contains("失眠")),
                "expected fallback to reflect the latest user message, not a stale cached insight"
        );
        verify(conversationStore).upsertInsight(eq("u1"), eq("c1"), eq(result));
        verify(analyticsWriter).recordInsightSnapshot(eq("u1"), eq("c1"), eq(result));
    }

    @Test
    void getInsight_notFound_returns404() {
        ConversationService service = service();
        when(conversationStore.findInsight("u1", "c1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getInsight("u1", "u1", "c1")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
