package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;

import java.util.List;
import java.util.Optional;

public interface ConversationStore {
    List<ConversationMessageDto> listMessages(String userId, String conversationId, int limit);

    void appendMessage(String userId, ConversationMessageDto message);

    Optional<ConversationInsightDto> findInsight(String userId, String conversationId);

    void upsertInsight(String userId, String conversationId, ConversationInsightDto insight);
}
