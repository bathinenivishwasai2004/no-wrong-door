package com.nowrongdoor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the No Wrong Door backend.
 * <p>
 * CORS is permissive in Phase 0 (dev only) to allow the frontend
 * to be served from any origin (file://, localhost:5500, etc.).
 * This MUST be locked down before production.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /**
     * Shared RestTemplate bean — adapters use this to call their
     * respective mock services.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * JSON ObjectMapper bean. Marked @Primary so general ObjectMapper injection
     * uses the JSON mapper, not the XmlMapper subclass.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    /**
     * XmlMapper bean for parsing XML payloads from the Benefits Register.
     */
    @Bean
    public com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapper() {
        return new com.fasterxml.jackson.dataformat.xml.XmlMapper();
    }
}
