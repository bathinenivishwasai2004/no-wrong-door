package com.nowrongdoor.adapters.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for the XML data source.
 * <p>
 * Phase 0 scope: prove connectivity by fetching the resident list
 * and returning the raw XML string. No retry logic, no timeout
 * handling, no XML-to-object transformation.
 * <p>
 * This adapter is intentionally isolated — it knows nothing about
 * the REST source adapter or any aggregation layer.
 */
@Component
public class XmlSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(XmlSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final XmlSourceConfig config;

    public XmlSourceAdapter(RestTemplate restTemplate, XmlSourceConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * Fetch all residents from the XML mock service.
     *
     * @return raw XML response as a String
     */
    public String fetchResidents() {
        String url = config.getBaseUrl() + "/residents";
        log.info("Fetching residents from XML source: {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("XML source responded with status {}", response.getStatusCode());
        return response.getBody();
    }

    /**
     * Health check — can we reach the XML mock service?
     * Uses the /health endpoint which does NOT have artificial
     * delays or failure simulation.
     *
     * @return true if the service responds with 2xx
     */
    public boolean checkHealth() {
        try {
            String url = config.getBaseUrl() + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("XML source health check failed: {}", e.getMessage());
            return false;
        }
    }
}
