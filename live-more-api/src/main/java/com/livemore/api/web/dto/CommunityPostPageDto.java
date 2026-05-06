package com.livemore.api.web.dto;

import java.util.List;

public class CommunityPostPageDto {
    private List<CommunityPostDto> items;
    private String nextCursor;
    private boolean hasMore;

    public List<CommunityPostDto> getItems() {
        return items;
    }

    public void setItems(List<CommunityPostDto> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
