package com.livemore.api.service;

import com.livemore.api.web.dto.JournalEntryDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JournalService {

    private final JournalStore journalStore;

    public JournalService(JournalStore journalStore) {
        this.journalStore = journalStore;
    }

    public List<JournalEntryDto> listForUser(String authenticatedUserId, String queryUserId) {
        assertSameUser(authenticatedUserId, queryUserId);
        return journalStore.listForUser(queryUserId);
    }

    public void replaceAllForUser(String authenticatedUserId, String queryUserId, List<JournalEntryDto> entries) {
        assertSameUser(authenticatedUserId, queryUserId);
        if (entries == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        Set<String> ids = entries.stream().map(JournalEntryDto::getId).collect(Collectors.toSet());
        if (ids.size() != entries.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate_entry_id");
        }
        for (JournalEntryDto dto : entries) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entry_id_required");
            }
        }
        journalStore.replaceAllForUser(queryUserId, entries);
    }

    private void assertSameUser(String authenticatedUserId, String queryUserId) {
        if (queryUserId == null || queryUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId_required");
        }
        if (!authenticatedUserId.equals(queryUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user_mismatch");
        }
    }
}
