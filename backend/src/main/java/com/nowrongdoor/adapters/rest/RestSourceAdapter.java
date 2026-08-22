package com.nowrongdoor.adapters.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for the paginated REST data source.
 * <p>
 * Phase 0 scope: prove connectivity by fetching one page of results
 * and returning the raw JSON string. No pagination-walking, no
 * deduplication, no transformation.
 * <p>
 * This adapter is intentionally isolated — it knows nothing about
 * the XML source adapter or any aggregation layer.
 */
@Component
public class RestSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(RestSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final RestSourceConfig config;

    public RestSourceAdapter(RestTemplate restTemplate, RestSourceConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * Fetch a single page of residents from the REST mock service.
     *
     * @param page page number (1-based)
     * @param size number of records per page
     * @return raw JSON response as a String
     */
    public String fetchResidents(int page, int size) {
        String url = config.getBaseUrl() + "/residents?page=" + page + "&size=" + size;
        log.info("Fetching residents from REST source: {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("REST source responded with status {}", response.getStatusCode());
        return response.getBody();
    }

    /**
     * Health check — can we reach the REST mock service?
     *
     * @return true if the service responds with 2xx
     */
    public boolean checkHealth() {
        try {
            String url = config.getBaseUrl() + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("REST source health check failed: {}", e.getMessage());
            return false;
        }
    }
}
