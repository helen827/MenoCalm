package com.livemore.api.service;

import com.livemore.api.web.dto.JournalEntryDto;

import java.util.List;

public interface JournalStore {
    List<JournalEntryDto> listForUser(String userId);

    void replaceAllForUser(String userId, List<JournalEntryDto> entries);
}
