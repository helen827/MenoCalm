package com.livemore.api.service;

import com.livemore.api.domain.CommunityMediaDocument;

public interface CommunityMediaStore {
    void upsertMedia(CommunityMediaDocument media);
}
