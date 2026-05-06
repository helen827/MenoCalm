package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@ConditionalOnMissingBean(ConversationAnalysisClient.class)
public class UnavailableConversationAnalysisClient implements ConversationAnalysisClient {
    @Override
    public ConversationInsightDto analyze(List<ConversationMessageDto> messages) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_not_configured");
    }
}
