package com.livemore.api.web;

import com.livemore.api.service.ConversationService;
import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/messages")
    public List<ConversationMessageDto> listMessages(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        return conversationService.listMessages(authentication.getName(), userId, conversationId, limit);
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConversationMessageDto appendMessage(
            @RequestParam("userId") String userId,
            @RequestBody ConversationMessageDto message,
            Authentication authentication
    ) {
        return conversationService.appendMessage(authentication.getName(), userId, message);
    }

    @GetMapping("/insight")
    public ConversationInsightDto getInsight(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId,
            Authentication authentication
    ) {
        return conversationService.getInsight(authentication.getName(), userId, conversationId);
    }

    @PutMapping(path = "/insight", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConversationInsightDto upsertInsight(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId,
            @RequestBody ConversationInsightDto body,
            Authentication authentication
    ) {
        return conversationService.upsertInsight(authentication.getName(), userId, conversationId, body);
    }

    @PostMapping("/insight/{conversationId}/analyze")
    public ConversationInsightDto analyzeInsight(
            @RequestParam("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            Authentication authentication
    ) {
        return conversationService.analyzeAndUpsertInsight(authentication.getName(), userId, conversationId);
    }
}
