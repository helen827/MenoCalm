package com.livemore.api.service;

import com.livemore.api.web.dto.KnowledgeDocDto;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocStore {
    List<KnowledgeDocDto> list(Integer limit);
    Optional<KnowledgeDocDto> findById(String id);
    KnowledgeDocDto upsert(KnowledgeDocDto doc);
    void deleteById(String id);
}
