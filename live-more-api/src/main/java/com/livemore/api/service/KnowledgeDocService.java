package com.livemore.api.service;

import com.livemore.api.web.dto.KnowledgeDocDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDocService {

    private final KnowledgeDocStore store;

    public KnowledgeDocService(KnowledgeDocStore store) {
        this.store = store;
    }

    public List<KnowledgeDocDto> list(Integer limit) {
        return store.list(limit);
    }

    public KnowledgeDocDto getById(String id) {
        return store.findById(requireNonBlank(id, "id_required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "knowledge_doc_not_found"));
    }

    public KnowledgeDocDto upsert(KnowledgeDocDto body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        String title = requireNonBlank(body.getTitle(), "title_required");
        String content = requireNonBlank(body.getContent(), "content_required");
        String id = body.getId();
        if (id == null || id.isBlank()) {
            id = "kdoc_" + UUID.randomUUID().toString().replace("-", "");
        }
        body.setId(id.trim());
        body.setTitle(title);
        body.setContent(content);
        if (body.getSource() == null) {
            body.setSource("");
        } else {
            body.setSource(body.getSource().trim());
        }
        if (body.getUpdatedAtMs() == null || body.getUpdatedAtMs() <= 0) {
            body.setUpdatedAtMs(System.currentTimeMillis());
        }
        return store.upsert(body);
    }

    public void deleteById(String id) {
        store.deleteById(requireNonBlank(id, "id_required"));
    }

    private String requireNonBlank(String value, String code) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
        }
        return value.trim();
    }
}
