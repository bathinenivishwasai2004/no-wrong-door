package com.nowrongdoor.adapters.xml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the XML source adapter.
 * Binds to {@code mock-services.xml.*} in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "mock-services.xml")
public class XmlSourceConfig {

    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
