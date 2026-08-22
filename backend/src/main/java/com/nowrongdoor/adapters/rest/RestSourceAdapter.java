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
 * Adapter for the official Calder County Resident Index (REST source).
 * <p>
 * Walks all pages using the {@code has_more} field from each response envelope.
 * The service intentionally has unstable page boundaries: the same resident can
 * appear on consecutive pages. Deduplication uses the stable {@code id} field
 * (first-seen wins, insertion order preserved via {@link LinkedHashMap}).
 * <p>
 * Pagination algorithm:
 * <ol>
 *   <li>Fetch page 1.</li>
 *   <li>Accumulate {@code results} into the dedup map keyed by {@code id}.</li>
 *   <li>If {@code has_more == true}, fetch the next page and repeat.</li>
 *   <li>Stop when {@code has_more == false}.</li>
 * </ol>
 * <p>
 * Returns a sealed {@link RestFetchResult} — never throws.
 */
@Component
public class RestSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(RestSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final RestSourceConfig config;
    private final ObjectMapper objectMapper;

    public RestSourceAdapter(RestTemplate restTemplate,
                             RestSourceConfig config,
                             @org.springframework.beans.factory.annotation.Qualifier("objectMapper") ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config       = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch all residents from the REST Resident Index.
     * <p>
     * Pages are walked via {@code has_more}. Records are deduplicated by
     * {@code id}; first-seen page wins. Insertion order is preserved.
     *
     * @return {@link RestFetchResult.Success} with stats, or {@link RestFetchResult.Failure}
     */
    public RestFetchResult fetchAllResidents() {
        int pageSize = config.getPageSize();
        // LinkedHashMap preserves insertion order — deterministic output
        LinkedHashMap<String, RestResident> seen = new LinkedHashMap<>();
        int pagesFetched = 0;
        int rawCount     = 0;
        int page         = 1;

        try {
            while (true) {
                RestPageResponse response = fetchPage(page, pageSize);
                if (response == null) {
                    if (page == 1) {
                        return new RestFetchResult.Failure(
                                "REST source returned empty or unparseable response on page 1");
                    }
                    log.warn("REST: page {} returned null — stopping early", page);
                    break;
                }

                List<RestResident> pageRecords = response.getResults();
                int n = pageRecords.size();
                rawCount += n;
                pagesFetched++;
                accumulate(pageRecords, seen);

                log.info("REST: page {} — {} records ({} unique so far, has_more={})",
                         page, n, seen.size(), response.isHasMore());

                if (!response.isHasMore()) {
                    break;
                }
                page++;
            }

            List<RestResident> result = new ArrayList<>(seen.values());
            int dropped = rawCount - result.size();
            log.info("REST: done — {} unique residents from {} pages ({} duplicates dropped)",
                     result.size(), pagesFetched, dropped);
            return new RestFetchResult.Success(result, pagesFetched, dropped);

        } catch (RestClientException e) {
            log.warn("REST source unreachable: {}", e.getMessage());
            return new RestFetchResult.Failure("REST source unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching REST source", e);
            return new RestFetchResult.Failure("Unexpected error: " + e.getMessage(), e);
        }
    }

    private RestPageResponse fetchPage(int page, int size) throws Exception {
        String url = config.getBaseUrl() + "/residents?page=" + page + "&page_size=" + size;
        log.debug("REST: fetching {}", url);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) return null;
        String body = response.getBody();
        if (body == null || body.isBlank()) return null;
        return objectMapper.readValue(body, RestPageResponse.class);
    }

    /**
     * Accumulate records into the seen map, keyed by {@code id}.
     * First-seen page wins; subsequent duplicates are silently skipped.
     * Records with a null {@code id} are warned and skipped.
     */
    private void accumulate(List<RestResident> incoming,
                            LinkedHashMap<String, RestResident> seen) {
        if (incoming == null) return;
        for (RestResident r : incoming) {
            if (r.getId() == null) {
                log.warn("REST: skipping record with null id");
                continue;
            }
            if (!seen.containsKey(r.getId())) {
                seen.put(r.getId(), r);
            } else {
                log.debug("REST: duplicate id={} dropped", r.getId());
            }
        }
    }

    /** Health check — the /health endpoint has no delay or failure simulation. */
    public boolean checkHealth() {
        try {
            ResponseEntity<String> r = restTemplate.getForEntity(
                    config.getBaseUrl() + "/health", String.class);
            return r.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("REST health check failed: {}", e.getMessage());
            return false;
        }
    }
}
