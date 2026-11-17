package com.ecom.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
@Configuration
public class ImageKitConfig {

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Bean(destroyMethod = "close")
    public ImageKitClient imageKitClient() {
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("IMAGEKIT_PRIVATE_KEY is not set.");
        }
        return ImageKitOkHttpClient.builder()
                .privateKey(privateKey)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }
}
