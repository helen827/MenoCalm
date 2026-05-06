package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;

import java.util.List;

public interface ConversationAnalysisClient {
    ConversationInsightDto analyze(List<ConversationMessageDto> messages);
}
