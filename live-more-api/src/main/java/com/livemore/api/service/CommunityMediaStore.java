package com.livemore.api.service;

import com.livemore.api.domain.CommunityMediaDocument;

import java.util.List;

public interface CommunityMediaStore {
    void upsertMedia(CommunityMediaDocument media);

    int countOwnedMedia(String ownerUserId, List<String> mediaIds);
}
