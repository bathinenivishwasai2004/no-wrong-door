package com.nowrongdoor.adapters.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the REST source adapter.
 * Binds to {@code mock-services.rest.*} in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "mock-services.rest")
public class RestSourceConfig {

    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
