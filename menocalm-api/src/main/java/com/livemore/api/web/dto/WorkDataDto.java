package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkDataDto {

    private boolean busy;
    private boolean tired;
    private int level;

    public WorkDataDto() {
    }

    @JsonProperty("busy")
    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    @JsonProperty("tired")
    public boolean isTired() {
        return tired;
    }

    public void setTired(boolean tired) {
        this.tired = tired;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
