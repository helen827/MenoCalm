package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityFeedFileDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityFeedService {

    private final CommunityFeedStore communityFeedStore;

    public CommunityFeedService(CommunityFeedStore communityFeedStore) {
        this.communityFeedStore = communityFeedStore;
    }

    public CommunityFeedFileDto getFeed() {
        return communityFeedStore.loadCurrent()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "community_feed_unavailable"
                ));
    }
}
