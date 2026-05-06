package com.livemore.api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final RefreshToken refreshToken = new RefreshToken();
    private final Community community = new Community();
    private final Security security = new Security();
    private final Auth auth = new Auth();
    private final Ai ai = new Ai();
    private final Storage storage = new Storage();
    private final Persistence persistence = new Persistence();

    public Jwt getJwt() {
        return jwt;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public Community getCommunity() {
        return community;
    }

    public Security getSecurity() {
        return security;
    }

    public Auth getAuth() {
        return auth;
    }

    public Ai getAi() {
        return ai;
    }

    public Storage getStorage() {
        return storage;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public static class Jwt {
        @NotBlank
        private String secret = "";
        @Min(60)
        private long accessTokenSeconds = 1800;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenSeconds() {
            return accessTokenSeconds;
        }

        public void setAccessTokenSeconds(long accessTokenSeconds) {
            this.accessTokenSeconds = accessTokenSeconds;
        }
    }

    public static class RefreshToken {
        @Min(3600)
        private long ttlSeconds = 2_592_000L;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Community {
        private boolean seedOnEmpty = false;
        @NotBlank
        private String seedClasspath = "classpath:data/community-feed.seed.json";

        public boolean isSeedOnEmpty() {
            return seedOnEmpty;
        }

        public void setSeedOnEmpty(boolean seedOnEmpty) {
            this.seedOnEmpty = seedOnEmpty;
        }

        public String getSeedClasspath() {
            return seedClasspath;
        }

        public void setSeedClasspath(String seedClasspath) {
            this.seedClasspath = seedClasspath;
        }
    }

    public static class Security {
        private String corsAllowedOrigins = "*";

        public String getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(String corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }
    }

    public static class Auth {
        private final LoginRateLimit loginRateLimit = new LoginRateLimit();
        private final Wechat wechat = new Wechat();
        private final Sms sms = new Sms();
        private boolean legacyPhoneLoginEnabled = false;
        private boolean testAccountLoginEnabled = false;
        private String testAccountSecret = "";

        public LoginRateLimit getLoginRateLimit() {
            return loginRateLimit;
        }

        public Wechat getWechat() {
            return wechat;
        }

        public Sms getSms() {
            return sms;
        }

        public boolean isLegacyPhoneLoginEnabled() {
            return legacyPhoneLoginEnabled;
        }

        public void setLegacyPhoneLoginEnabled(boolean legacyPhoneLoginEnabled) {
            this.legacyPhoneLoginEnabled = legacyPhoneLoginEnabled;
        }

        public boolean isTestAccountLoginEnabled() {
            return testAccountLoginEnabled;
        }

        public void setTestAccountLoginEnabled(boolean testAccountLoginEnabled) {
            this.testAccountLoginEnabled = testAccountLoginEnabled;
        }

        public String getTestAccountSecret() {
            return testAccountSecret;
        }

        public void setTestAccountSecret(String testAccountSecret) {
            this.testAccountSecret = testAccountSecret;
        }
    }

    public static class LoginRateLimit {
        @Min(1)
        private long perPhoneMax = 8;
        @Min(1)
        private long perIpMax = 20;
        @Min(30)
        private long windowSeconds = 300;

        public long getPerPhoneMax() {
            return perPhoneMax;
        }

        public void setPerPhoneMax(long perPhoneMax) {
            this.perPhoneMax = perPhoneMax;
        }

        public long getPerIpMax() {
            return perIpMax;
        }

        public void setPerIpMax(long perIpMax) {
            this.perIpMax = perIpMax;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public static class Wechat {
        private String appId = "";
        private String appSecret = "";
        private String universalLink = "";
        private String redirectUri = "";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getUniversalLink() {
            return universalLink;
        }

        public void setUniversalLink(String universalLink) {
            this.universalLink = universalLink;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class Sms {
        private String provider = "aliyun";
        private String signName = "";
        private String templateCode = "";
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String regionId = "cn-hangzhou";
        private final Verification verification = new Verification();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getRegionId() {
            return regionId;
        }

        public void setRegionId(String regionId) {
            this.regionId = regionId;
        }

        public Verification getVerification() {
            return verification;
        }
    }

    public static class Ai {
        private String provider = "";
        private String endpoint = "";
        private String apiKey = "";
        private String model = "";
        private String modelFallbacks = "";
        @Min(1000)
        private long connectTimeoutMs = 5000;
        @Min(1000)
        private long readTimeoutMs = 15000;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getModelFallbacks() {
            return modelFallbacks;
        }

        public void setModelFallbacks(String modelFallbacks) {
            this.modelFallbacks = modelFallbacks;
        }

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public long getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(long readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Verification {
        @Min(60)
        private long codeTtlSeconds = 300;
        @Min(10)
        private long resendCooldownSeconds = 60;
        @Min(1)
        private long perPhonePerHourMax = 6;
        @Min(1)
        private long perIpPerHourMax = 20;
        @Min(1)
        private long maxVerifyAttempts = 8;

        public long getCodeTtlSeconds() {
            return codeTtlSeconds;
        }

        public void setCodeTtlSeconds(long codeTtlSeconds) {
            this.codeTtlSeconds = codeTtlSeconds;
        }

        public long getResendCooldownSeconds() {
            return resendCooldownSeconds;
        }

        public void setResendCooldownSeconds(long resendCooldownSeconds) {
            this.resendCooldownSeconds = resendCooldownSeconds;
        }

        public long getPerPhonePerHourMax() {
            return perPhonePerHourMax;
        }

        public void setPerPhonePerHourMax(long perPhonePerHourMax) {
            this.perPhonePerHourMax = perPhonePerHourMax;
        }

        public long getPerIpPerHourMax() {
            return perIpPerHourMax;
        }

        public void setPerIpPerHourMax(long perIpPerHourMax) {
            this.perIpPerHourMax = perIpPerHourMax;
        }

        public long getMaxVerifyAttempts() {
            return maxVerifyAttempts;
        }

        public void setMaxVerifyAttempts(long maxVerifyAttempts) {
            this.maxVerifyAttempts = maxVerifyAttempts;
        }
    }

    public static class Storage {
        private String s3Endpoint = "";
        private String s3Bucket = "";
        private String s3AccessKey = "";
        private String s3SecretKey = "";
        private String s3Region = "us-east-1";
        @Min(60)
        private long presignTtlSeconds = 600;
        private String publicBaseUrl = "";

        public String getS3Endpoint() {
            return s3Endpoint;
        }

        public void setS3Endpoint(String s3Endpoint) {
            this.s3Endpoint = s3Endpoint;
        }

        public String getS3Bucket() {
            return s3Bucket;
        }

        public void setS3Bucket(String s3Bucket) {
            this.s3Bucket = s3Bucket;
        }

        public String getS3AccessKey() {
            return s3AccessKey;
        }

        public void setS3AccessKey(String s3AccessKey) {
            this.s3AccessKey = s3AccessKey;
        }

        public String getS3SecretKey() {
            return s3SecretKey;
        }

        public void setS3SecretKey(String s3SecretKey) {
            this.s3SecretKey = s3SecretKey;
        }

        public String getS3Region() {
            return s3Region;
        }

        public void setS3Region(String s3Region) {
            this.s3Region = s3Region;
        }

        public long getPresignTtlSeconds() {
            return presignTtlSeconds;
        }

        public void setPresignTtlSeconds(long presignTtlSeconds) {
            this.presignTtlSeconds = presignTtlSeconds;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class Persistence {
        private final Mysql mysql = new Mysql();

        public Mysql getMysql() {
            return mysql;
        }
    }

    public static class Mysql {
        private String url = "";
        private String user = "";
        private String password = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
