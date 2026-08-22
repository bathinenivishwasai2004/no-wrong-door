package com.nowrongdoor.adapters.xml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the XML Benefits Register adapter.
 * Binds to {@code mock-services.xml.*} in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "mock-services.xml")
public class XmlSourceConfig {

    private String baseUrl;
    private int timeoutMs     = 5000;
    private int maxRetries    = 3;
    private int initialBackoffMs = 500;

    public String getBaseUrl()          { return baseUrl; }
    public int getTimeoutMs()           { return timeoutMs; }
    public int getMaxRetries()          { return maxRetries; }
    public int getInitialBackoffMs()    { return initialBackoffMs; }

    public void setBaseUrl(String v)        { this.baseUrl = v; }
    public void setTimeoutMs(int v)         { this.timeoutMs = v; }
    public void setMaxRetries(int v)        { this.maxRetries = v; }
    public void setInitialBackoffMs(int v)  { this.initialBackoffMs = v; }
}
