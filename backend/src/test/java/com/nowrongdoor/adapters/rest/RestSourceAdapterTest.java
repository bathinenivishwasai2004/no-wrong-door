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
 * Requires the official REST mock service to be running on port 8081.
 * Start it with: {@code python mock-services/rest-source/rest_service.py}
 * <p>
 * The official service serves 620 records across paginated pages of 25.
 * It intentionally has unstable boundaries; the adapter must deduplicate
 * by {@code id} and return exactly 620 unique residents.
 */
@SpringBootTest
class RestSourceAdapterTest {

    @Autowired
    private RestSourceAdapter restSourceAdapter;

    // ── Health check ───────────────────────────────────────────────────────

    @Test
    void healthCheck_returnsTrue_whenMockServiceIsRunning() {
        boolean healthy = restSourceAdapter.checkHealth();
        assertTrue(healthy,
                "REST mock service should be reachable on http://localhost:8081. "
                + "Start it with: python mock-services/rest-source/rest_service.py");
    }

    // ── Full pagination + deduplication ───────────────────────────────────

    @Test
    void fetchAllResidents_returnsDeduplicatedResults_whenMockServiceIsRunning() {
        RestFetchResult result = restSourceAdapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result,
                "Expected Success but got: " + result);

        RestFetchResult.Success success = (RestFetchResult.Success) result;
        List<RestResident> residents = success.residents();

        // Official dataset has exactly 620 unique residents
        assertEquals(620, residents.size(),
                "Expected exactly 620 unique residents after pagination + deduplication, "
                + "but got: " + residents.size());

        // Verify deduplication: no two residents share the same id
        Set<String> ids = residents.stream()
                .map(RestResident::getId)
                .collect(Collectors.toSet());
        assertEquals(residents.size(), ids.size(),
                "Duplicate IDs found — deduplication by id failed. "
                + "duplicatesDropped=" + success.duplicatesDropped());

        // Every resident must have required non-blank fields
        for (RestResident r : residents) {
            assertNotNull(r.getId(),
                    "Resident ID must not be null");
            assertFalse(r.getId().isBlank(),
                    "Resident ID must not be blank");
            assertNotNull(r.getFirstName(),
                    "first_name must not be null for id=" + r.getId());
            assertNotNull(r.getLastName(),
                    "last_name must not be null for id=" + r.getId());
            assertNotNull(r.getDateOfBirth(),
                    "date_of_birth must not be null for id=" + r.getId());
        }

        // Log pagination stats for visibility
        System.out.printf(
                "REST integration: %d unique residents from %d pages (%d duplicates dropped)%n",
                residents.size(), success.pagesFetched(), success.duplicatesDropped());
    }
}
