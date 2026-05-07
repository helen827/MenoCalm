package com.livemore.api.service;

import com.livemore.api.domain.CommunityMediaDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CommunityMediaStore {
    void upsertMedia(CommunityMediaDocument media);

    int countOwnedMedia(String ownerUserId, List<String> mediaIds);

    /**
     * Returns one entry per distinct id found; ids not present in DB are omitted.
     */
    Map<String, CommunityMediaDocument> findByIds(Collection<String> mediaIds);

    /**
     * @return number of rows updated (0 if not found, wrong owner, or wrong current status)
     */
    int markMediaReadyIfPending(String mediaId, String ownerUserId);
}
