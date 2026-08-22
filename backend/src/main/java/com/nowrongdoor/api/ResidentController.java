package com.nowrongdoor.api;

import com.nowrongdoor.adapters.rest.RestFetchResult;
import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resident search endpoint.
 * <p>
 * Phase 1: calls the REST adapter, walks all pages (dedup handled in the
 * adapter), then filters results by the query string (case-insensitive
 * substring match on first + last name).
 * <p>
 * Response shape is backwards-compatible with Phase 0 so the frontend
 * requires only minor additions to display the extra fields.
 */
@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private static final Logger log = LoggerFactory.getLogger(ResidentController.class);

    private final RestSourceAdapter restAdapter;

    public ResidentController(RestSourceAdapter restAdapter) {
        this.restAdapter = restAdapter;
    }

    /**
     * GET /api/residents/search?query=...
     * <p>
     * Fetches all residents from the REST source (paginated + deduplicated),
     * then filters by a case-insensitive substring match on the full name.
     * <p>
     * Response fields:
     * <ul>
     *   <li>{@code query}        — echoed back from the request parameter</li>
     *   <li>{@code results}      — list of matched residents from REST source</li>
     *   <li>{@code totalResults} — count of matched residents</li>
     *   <li>{@code restTotal}    — total unique records fetched before filtering</li>
     *   <li>{@code message}      — human-readable status message</li>
     *   <li>{@code source}       — always {@code "rest-source"} in Phase 1</li>
     * </ul>
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(defaultValue = "") String query) {

        // ── Empty query — return empty result immediately ──────────────────
        if (query.isBlank()) {
            return ResponseEntity.ok(buildResponse(query, List.of(), 0,
                    "Enter a name to search for residents", null));
        }

        // ── Fetch all residents from REST source ───────────────────────────
        log.info("Search request: query='{}'", query);
        RestFetchResult result = restAdapter.fetchAllResidents();

        switch (result) {
            case RestFetchResult.Failure failure -> {
                log.warn("REST source fetch failed for query='{}': {}", query, failure.message());
                Map<String, Object> errorBody = new LinkedHashMap<>();
                errorBody.put("query", query);
                errorBody.put("error", failure.message());
                errorBody.put("source", "rest-source");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody);
            }

            case RestFetchResult.Success success -> {
                List<RestResident> all = success.residents();
                String lowerQuery = query.toLowerCase(Locale.ROOT);

                // Case-insensitive substring match on first + last name
                List<Map<String, String>> matched = all.stream()
                        .filter(r -> matches(r, lowerQuery))
                        .map(this::toMap)
                        .collect(Collectors.toList());

                log.info("Search query='{}': {} match(es) from {} unique REST records",
                         query, matched.size(), all.size());

                return ResponseEntity.ok(
                        buildResponse(query, matched, matched.size(),
                                matched.isEmpty()
                                        ? "No residents found matching \"" + query + "\""
                                        : "Found " + matched.size() + " resident(s) from REST source",
                                all.size()));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean matches(RestResident r, String lowerQuery) {
        String fullName = ((r.getFirstName() != null ? r.getFirstName() : "") +
                           " " +
                           (r.getLastName() != null ? r.getLastName() : ""))
                          .toLowerCase(Locale.ROOT);
        return fullName.contains(lowerQuery);
    }

    private Map<String, String> toMap(RestResident r) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id",          r.getId());
        m.put("firstName",   r.getFirstName());
        m.put("lastName",    r.getLastName());
        m.put("dateOfBirth", r.getDateOfBirth());
        m.put("address",     r.getAddress());
        m.put("phone",       r.getPhone());
        m.put("source",      "rest-source");
        return m;
    }

    private Map<String, Object> buildResponse(String query,
                                              List<?> results,
                                              int totalResults,
                                              String message,
                                              Integer restTotal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query",        query);
        response.put("results",      results);
        response.put("totalResults", totalResults);
        if (restTotal != null) {
            response.put("restTotal", restTotal);
        }
        response.put("message",      message);
        response.put("source",       "rest-source");
        return response;
    }
}
