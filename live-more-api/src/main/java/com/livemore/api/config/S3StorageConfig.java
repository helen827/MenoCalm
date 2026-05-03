package com.livemore.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3StorageConfig {

    @Bean
    public S3Presigner s3Presigner(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.getStorage();
        AwsBasicCredentials creds = AwsBasicCredentials.create(storage.getS3AccessKey(), storage.getS3SecretKey());

        S3Presigner.Builder builder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(storage.getS3Region()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());

        if (storage.getS3Endpoint() != null && !storage.getS3Endpoint().isBlank()) {
            builder.endpointOverride(URI.create(storage.getS3Endpoint()));
        }
        return builder.build();
    }
}
