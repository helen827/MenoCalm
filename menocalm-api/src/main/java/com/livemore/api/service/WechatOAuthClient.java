package com.livemore.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WechatOAuthClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatOAuthClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public WechatOAuthProfile exchangeCode(String code) {
        AppProperties.Wechat cfg = appProperties.getAuth().getWechat();
        if (isBlank(cfg.getAppId()) || isBlank(cfg.getAppSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "wechat_not_configured");
        }
        JsonNode token = requestJson(buildTokenUrl(cfg, code));
        if (token.has("errcode")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_wechat_code");
        }
        String openId = text(token, "openid");
        String accessToken = text(token, "access_token");
        String unionId = nullableText(token, "unionid");
        if (openId == null || accessToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_wechat_code");
        }

        WechatOAuthProfile profile = new WechatOAuthProfile();
        profile.setOpenId(openId);
        profile.setUnionId(unionId);

        JsonNode userInfo = requestJson(buildUserInfoUrl(accessToken, openId));
        if (!userInfo.has("errcode")) {
            profile.setNickname(nullableText(userInfo, "nickname"));
            if (profile.getUnionId() == null) {
                profile.setUnionId(nullableText(userInfo, "unionid"));
            }
        }
        return profile;
    }

    private JsonNode requestJson(String url) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "wechat_upstream_failed");
            }
            return objectMapper.readTree(resp.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "wechat_upstream_failed");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "wechat_upstream_failed");
        }
    }

    private String buildTokenUrl(AppProperties.Wechat cfg, String code) {
        return "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + enc(cfg.getAppId())
                + "&secret=" + enc(cfg.getAppSecret())
                + "&code=" + enc(code)
                + "&grant_type=authorization_code";
    }

    private String buildUserInfoUrl(String accessToken, String openId) {
        return "https://api.weixin.qq.com/sns/userinfo"
                + "?access_token=" + enc(accessToken)
                + "&openid=" + enc(openId)
                + "&lang=zh_CN";
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private String nullableText(JsonNode node, String field) {
        String value = text(node, field);
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
