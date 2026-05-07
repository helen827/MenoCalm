package com.livemore.api.web.dto;

import java.util.ArrayList;
import java.util.List;

public class CommunityFeedFileDto {

    private List<CommunityFeedItemDto> official = new ArrayList<>();
    private List<CommunityFeedItemDto> user = new ArrayList<>();

    public List<CommunityFeedItemDto> getOfficial() {
        return official;
    }

    public void setOfficial(List<CommunityFeedItemDto> official) {
        this.official = official;
    }

    public List<CommunityFeedItemDto> getUser() {
        return user;
    }

    public void setUser(List<CommunityFeedItemDto> user) {
        this.user = user;
    }
}
