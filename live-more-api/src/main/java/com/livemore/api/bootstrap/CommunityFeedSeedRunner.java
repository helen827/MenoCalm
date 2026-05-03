package com.livemore.api.bootstrap;

import com.livemore.api.config.AppProperties;
import com.livemore.api.service.CommunityFeedStore;
import com.livemore.api.web.dto.CommunityFeedFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Optional one-time import of feed JSON into MySQL when the snapshot is absent.
 * Production should normally load content via CMS/admin pipelines; enable only with explicit configuration.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommunityFeedSeedRunner implements ApplicationRunner {

    private final AppProperties appProperties;
    private final CommunityFeedStore communityFeedStore;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public CommunityFeedSeedRunner(
            AppProperties appProperties,
            CommunityFeedStore communityFeedStore,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.communityFeedStore = communityFeedStore;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!appProperties.getCommunity().isSeedOnEmpty()) {
            return;
        }
        if (communityFeedStore.existsCurrent()) {
            return;
        }
        Resource resource = resourceLoader.getResource(appProperties.getCommunity().getSeedClasspath());
        if (!resource.exists()) {
            throw new IllegalStateException("Community seed resource missing: " + appProperties.getCommunity().getSeedClasspath());
        }
        CommunityFeedFileDto file = objectMapper.readValue(resource.getInputStream(), CommunityFeedFileDto.class);
        communityFeedStore.upsertCurrent(file);
    }
}
