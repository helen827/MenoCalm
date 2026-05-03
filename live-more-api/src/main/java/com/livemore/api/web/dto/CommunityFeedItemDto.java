package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunityFeedItemDto {

    private String id;
    private String title;
    private String tag;
    private double cardHeight;
    private boolean official;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public double getCardHeight() {
        return cardHeight;
    }

    public void setCardHeight(double cardHeight) {
        this.cardHeight = cardHeight;
    }

    @JsonProperty("isOfficial")
    public boolean isOfficial() {
        return official;
    }

    @JsonProperty("isOfficial")
    public void setOfficial(boolean official) {
        this.official = official;
    }
}
