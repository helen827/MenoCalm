package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private String code;
    private String message;
    private String requestId;
    private String path;
    private long timestamp;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(String code, String message, String requestId, String path, long timestamp) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
