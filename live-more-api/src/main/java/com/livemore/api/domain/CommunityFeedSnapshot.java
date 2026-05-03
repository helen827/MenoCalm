package com.livemore.api.domain;

import com.livemore.api.web.dto.CommunityFeedItemDto;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

public class CommunityFeedSnapshot {

    public static final String SINGLETON_ID = "current";

    @Id
    private String id = SINGLETON_ID;

    private List<CommunityFeedItemDto> official = new ArrayList<>();
    private List<CommunityFeedItemDto> user = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
