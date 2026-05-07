package com.livemore.api.web;

import com.livemore.api.service.JournalService;
import com.livemore.api.web.dto.JournalEntryDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/journal", produces = MediaType.APPLICATION_JSON_VALUE)
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/entries")
    public List<JournalEntryDto> list(
            @RequestParam("userId") String userId,
            Authentication authentication
    ) {
        return journalService.listForUser(authentication.getName(), userId);
    }

    @PutMapping(path = "/entries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void replace(
            @RequestParam("userId") String userId,
            @RequestBody List<JournalEntryDto> body,
            Authentication authentication
    ) {
        journalService.replaceAllForUser(authentication.getName(), userId, body);
    }
}
