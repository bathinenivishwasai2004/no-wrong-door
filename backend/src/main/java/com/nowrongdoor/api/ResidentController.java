package com.nowrongdoor.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resident search endpoint.
 * <p>
 * Phase 0: returns a hardcoded stub response to prove the
 * frontend-to-backend wiring works. Real search logic (calling
 * adapters, merging, dedup) comes in Phase 1.
 */
@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    /**
     * GET /api/residents/search?query=...
     * <p>
     * Returns stub data for Phase 0. The frontend should be able
     * to render the results panel, loading state, and empty state
     * using this endpoint.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(defaultValue = "") String query) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);

        if (query.isBlank()) {
            response.put("results", List.of());
            response.put("totalResults", 0);
            response.put("message", "Enter a name to search for residents");
            return ResponseEntity.ok(response);
        }

        // Stub data — hardcoded results for Phase 0
        List<Map<String, String>> stubResults = List.of(
                Map.of(
                        "id", "R001",
                        "firstName", "Maria",
                        "lastName", "Garcia",
                        "dateOfBirth", "1985-03-14",
                        "source", "stub"
                ),
                Map.of(
                        "id", "R002",
                        "firstName", "James",
                        "lastName", "Johnson",
                        "dateOfBirth", "1990-07-22",
                        "source", "stub"
                ),
                Map.of(
                        "id", "R009",
                        "firstName", "Thomas",
                        "lastName", "Anderson",
                        "dateOfBirth", "1982-02-19",
                        "source", "stub"
                )
        );

        response.put("results", stubResults);
        response.put("totalResults", stubResults.size());
        response.put("message", "Phase 0 stub — real search coming in Phase 1");

        return ResponseEntity.ok(response);
    }
}
