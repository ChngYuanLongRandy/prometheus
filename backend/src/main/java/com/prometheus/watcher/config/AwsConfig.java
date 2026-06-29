package com.prometheus.watcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS SDK v2 clients. Credentials are resolved from the environment via
 * DefaultCredentialsProvider (reads AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY).
 * No credentials are injected in code.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:ap-southeast-1}")
    private String region;

    @Bean
    public PollyClient pollyClient() {
        return PollyClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
