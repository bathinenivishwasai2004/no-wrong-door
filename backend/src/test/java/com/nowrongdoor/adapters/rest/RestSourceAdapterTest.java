package com.nowrongdoor.adapters.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the REST source adapter.
 * <p>
 * Requires the REST mock service to be running on port 3001.
 * Start it with: cd mock-services/rest-source && npm start
 * <p>
 * Phase 0 tests (kept intact): health check and single-page raw fetch.
 * Phase 1 tests: full pagination walk and deduplication via fetchAllResidents().
 */
@SpringBootTest
class RestSourceAdapterTest {

    @Autowired
    private RestSourceAdapter restSourceAdapter;

    // ── Phase 0 tests — unchanged ──────────────────────────────────────────

    @Test
    void healthCheck_returnsTrue_whenMockServiceIsRunning() {
        boolean healthy = restSourceAdapter.checkHealth();
        assertTrue(healthy, "REST mock service should be reachable on port 3001. "
                + "Start it with: cd mock-services/rest-source && npm start");
    }

    @Test
    void fetchResidents_returnsJsonWithData_whenMockServiceIsRunning() {
        String response = restSourceAdapter.fetchResidents(1, 5);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isBlank(), "Response should not be blank");

        // Verify it looks like the expected JSON structure
        assertTrue(response.contains("\"data\""),
                "Response should contain a 'data' field");
        assertTrue(response.contains("\"page\""),
                "Response should contain a 'page' field");
        assertTrue(response.contains("\"totalRecords\""),
                "Response should contain a 'totalRecords' field");
    }

    // ── Phase 1 test — pagination + deduplication ──────────────────────────

    @Test
    void fetchAllResidents_returnsDeduplicatedResults_whenMockServiceIsRunning() {
        RestFetchResult result = restSourceAdapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result,
                "Expected Success but got: " + result);

        List<RestResident> residents = ((RestFetchResult.Success) result).residents();

        // Mock has 20 seed records; we must get at least that many
        assertTrue(residents.size() >= 20,
                "Expected at least 20 unique residents but got: " + residents.size());

        // No duplicate IDs — deduplication must have worked
        Set<String> ids = residents.stream()
                .map(RestResident::getId)
                .collect(Collectors.toSet());
        assertEquals(residents.size(), ids.size(),
                "Duplicate IDs found in result — deduplication failed");

        // Every resident must have a non-blank ID and name
        for (RestResident r : residents) {
            assertNotNull(r.getId(),        "Resident ID must not be null");
            assertFalse(r.getId().isBlank(), "Resident ID must not be blank");
            assertNotNull(r.getFirstName(), "First name must not be null for " + r.getId());
            assertNotNull(r.getLastName(),  "Last name must not be null for " + r.getId());
        }
    }
}
