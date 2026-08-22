package com.nowrongdoor.adapters.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Adapter for the paginated REST data source.
 * <p>
 * Phase 1 responsibilities:
 * <ul>
 *   <li>Walk all pages until {@code totalPages} is exhausted.</li>
 *   <li>Deduplicate records across pages using the stable {@code id} field.</li>
 *   <li>Return a sealed {@link RestFetchResult} — never throws.</li>
 * </ul>
 * <p>
 * <strong>Deduplication strategy:</strong> records are accumulated into a
 * {@code LinkedHashMap<String, RestResident>} keyed by {@code id}. If the same
 * {@code id} appears on multiple pages (as injected by the mock), the first
 * occurrence is kept and subsequent duplicates are silently discarded.
 * Insertion order is preserved, so results are deterministic for a given
 * source state.
 * <p>
 * <strong>Pagination termination:</strong> the loop reads {@code totalPages} from
 * the first response envelope and iterates from page 1 through {@code totalPages}.
 * The page count is never hard-coded; if the source changes the number of records
 * or page size, the adapter adapts automatically.
 * <p>
 * This adapter is intentionally isolated — it knows nothing about the XML adapter
 * or any aggregation layer.
 */
@Component
public class RestSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(RestSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final RestSourceConfig config;
    private final ObjectMapper objectMapper;

    public RestSourceAdapter(RestTemplate restTemplate,
                             RestSourceConfig config,
                             ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config       = config;
        this.objectMapper = objectMapper;
    }

    // ── Phase 1: full pagination + deduplication ───────────────────────────

    /**
     * Fetch all residents from the REST source by walking every page.
     * <p>
     * Uses the {@code totalPages} field in the first response to determine
     * how many pages exist, then fetches each one. Duplicates are removed
     * using the resident's {@code id} as the stable unique key.
     *
     * @return {@link RestFetchResult.Success} with deduplicated residents,
     *         or {@link RestFetchResult.Failure} if any error occurs
     */
    public RestFetchResult fetchAllResidents() {
        int pageSize = config.getPageSize();
        // LinkedHashMap preserves insertion order → deterministic output
        LinkedHashMap<String, RestResident> seen = new LinkedHashMap<>();

        try {
            // ── Page 1 — also determines totalPages ────────────────────────
            RestPageResponse firstPage = fetchPage(1, pageSize);
            if (firstPage == null) {
                return new RestFetchResult.Failure("REST source returned empty or unparseable response");
            }

            accumulate(firstPage.getData(), seen);
            int totalPages = firstPage.getTotalPages();
            log.info("REST source: page 1/{} fetched, {} records (total unique so far: {})",
                     totalPages, firstPage.getData().size(), seen.size());

            // ── Pages 2..totalPages ────────────────────────────────────────
            for (int page = 2; page <= totalPages; page++) {
                RestPageResponse next = fetchPage(page, pageSize);
                if (next == null) {
                    log.warn("REST source: page {} returned null — stopping early", page);
                    break;
                }
                accumulate(next.getData(), seen);
                log.info("REST source: page {}/{} fetched, {} records (total unique so far: {})",
                         page, totalPages, next.getData().size(), seen.size());
            }

            List<RestResident> result = new ArrayList<>(seen.values());
            log.info("REST source: pagination complete — {} unique residents collected", result.size());
            return new RestFetchResult.Success(result);

        } catch (RestClientException e) {
            log.warn("REST source unreachable: {}", e.getMessage());
            return new RestFetchResult.Failure("REST source is unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching from REST source", e);
            return new RestFetchResult.Failure("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch a single page and deserialize into {@link RestPageResponse}.
     * Returns {@code null} if the body is null or cannot be parsed.
     */
    private RestPageResponse fetchPage(int page, int size) throws Exception {
        String url = config.getBaseUrl() + "/residents?page=" + page + "&size=" + size;
        log.debug("REST source: fetching {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("REST source: page {} returned HTTP {}", page, response.getStatusCode());
            return null;
        }

        String body = response.getBody();
        if (body == null || body.isBlank()) {
            return null;
        }

        return objectMapper.readValue(body, RestPageResponse.class);
    }

    /**
     * Add residents from {@code incoming} into {@code seen}, skipping any
     * whose {@code id} is already present (first-seen-wins deduplication).
     */
    private void accumulate(List<RestResident> incoming,
                            LinkedHashMap<String, RestResident> seen) {
        if (incoming == null) return;
        for (RestResident r : incoming) {
            if (r.getId() == null) {
                log.warn("REST source: skipping resident with null id");
                continue;
            }
            if (seen.containsKey(r.getId())) {
                log.debug("REST source: duplicate id={} discarded", r.getId());
            } else {
                seen.put(r.getId(), r);
            }
        }
    }

    // ── Phase 0 methods — kept for backward compatibility ──────────────────

    /**
     * Fetch a single page of residents from the REST mock service.
     * <p>
     * Retained from Phase 0 so existing integration tests continue to pass.
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
