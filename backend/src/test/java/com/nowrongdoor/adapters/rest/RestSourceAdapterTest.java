package com.nowrongdoor.adapters.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the REST source adapter.
 * <p>
 * Requires the REST mock service to be running on port 3001.
 * Start it with: cd mock-services/rest-source && npm start
 */
@SpringBootTest
class RestSourceAdapterTest {

    @Autowired
    private RestSourceAdapter restSourceAdapter;

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
}
