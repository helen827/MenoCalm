package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityFeedFileDto;

import java.util.Optional;

public interface CommunityFeedStore {
    Optional<CommunityFeedFileDto> loadCurrent();

    boolean existsCurrent();

    void upsertCurrent(CommunityFeedFileDto feed);
}
