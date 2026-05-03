package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.web.dto.ExtractedDataDto;
import com.livemore.api.web.dto.JournalEntryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalStore journalStore;

    private JournalService serviceWithDefaultConfig() {
        return new JournalService(journalStore);
    }

    @Test
    void listForUser_userMismatch_throwsForbidden() {
        JournalService service = serviceWithDefaultConfig();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.listForUser("phone_13800138000", "phone_13900000000")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void replaceAllForUser_duplicateIds_throwsBadRequest() {
        JournalService service = serviceWithDefaultConfig();

        JournalEntryDto a = entry("e-1", "text-a", null);
        JournalEntryDto b = entry("e-1", "text-b", new ExtractedDataDto());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.replaceAllForUser("phone_13800138000", "phone_13800138000", List.of(a, b))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(journalStore, never()).replaceAllForUser(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void replaceAllForUser_savesEntriesAndFillsDefaultExtracted() {
        JournalService service = serviceWithDefaultConfig();
        JournalEntryDto a = entry("e-1", "text-a", null);
        JournalEntryDto b = entry("e-2", "text-b", new ExtractedDataDto());

        List<JournalEntryDto> entries = List.of(a, b);
        service.replaceAllForUser("phone_13800138000", "phone_13800138000", entries);
        verify(journalStore).replaceAllForUser("phone_13800138000", entries);
    }

    @Test
    void listForUser_mapsRepositoryDocuments() {
        JournalService service = serviceWithDefaultConfig();
        JournalEntryDto dto = new JournalEntryDto();
        dto.setId("e-1");
        dto.setRawText("abc");
        when(journalStore.listForUser("phone_13800138000")).thenReturn(List.of(dto));

        List<JournalEntryDto> result = service.listForUser("phone_13800138000", "phone_13800138000");
        assertEquals(1, result.size());
        assertEquals("e-1", result.get(0).getId());
        assertEquals("abc", result.get(0).getRawText());
    }

    @Test
    void replaceAllForUser_delegatesToStore() {
        JournalService service = serviceWithDefaultConfig();
        JournalEntryDto a = entry("e-1", "text-a", null);
        List<JournalEntryDto> entries = List.of(a);

        service.replaceAllForUser("phone_13800138000", "phone_13800138000", entries);

        verify(journalStore).replaceAllForUser("phone_13800138000", entries);
    }

    @Test
    void listForUser_usesStoreResult() {
        JournalService service = serviceWithDefaultConfig();
        JournalEntryDto dto = entry("e-100", "mysql", new ExtractedDataDto());
        when(journalStore.listForUser("phone_13800138000")).thenReturn(List.of(dto));

        List<JournalEntryDto> result = service.listForUser("phone_13800138000", "phone_13800138000");
        assertEquals(1, result.size());
        assertEquals("e-100", result.get(0).getId());
        verify(journalStore).listForUser("phone_13800138000");
    }

    private static JournalEntryDto entry(String id, String rawText, ExtractedDataDto extracted) {
        JournalEntryDto dto = new JournalEntryDto();
        dto.setId(id);
        dto.setDate("2026-05-03");
        dto.setRawText(rawText);
        dto.setCreatedAt(System.currentTimeMillis());
        dto.setRevision(1);
        dto.setExtracted(extracted);
        return dto;
    }
}
