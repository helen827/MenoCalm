package com.livemore.api.web;

import com.livemore.api.service.KnowledgeDocService;
import com.livemore.api.web.dto.KnowledgeDocDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
public class KnowledgeDocController {

    private final KnowledgeDocService service;

    public KnowledgeDocController(KnowledgeDocService service) {
        this.service = service;
    }

    @GetMapping("/docs")
    public List<KnowledgeDocDto> list(@RequestParam(value = "limit", required = false) Integer limit) {
        return service.list(limit);
    }

    @GetMapping("/docs/{id}")
    public KnowledgeDocDto get(@PathVariable("id") String id) {
        return service.getById(id);
    }

    @PostMapping(path = "/docs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public KnowledgeDocDto upsert(@RequestBody KnowledgeDocDto body) {
        return service.upsert(body);
    }

    @DeleteMapping("/docs/{id}")
    public void delete(@PathVariable("id") String id) {
        service.deleteById(id);
    }
}
