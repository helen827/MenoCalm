package com.livemore.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Non-test environments must use a strong, unique JWT secret from environment variables.
 */
@Component
@Profile("!test")
public class JwtSecretStartupValidator implements ApplicationRunner {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String DEV_PLACEHOLDER = "dev-only-do-not-use-in-shared-or-production-env-min-32-chars";

    private final AppProperties appProperties;

    public JwtSecretStartupValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is required (set JWT_SECRET)");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        if (DEV_PLACEHOLDER.equals(secret)) {
            throw new IllegalStateException("app.jwt.secret must not use development placeholder");
        }
    }
}
