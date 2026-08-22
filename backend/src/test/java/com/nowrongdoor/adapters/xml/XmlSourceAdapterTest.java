package com.nowrongdoor.adapters.xml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the XML source adapter.
 * <p>
 * Requires the XML mock service to be running on port 3002.
 * Start it with: cd mock-services/xml-source && npm start
 * <p>
 * Note: The XML mock has ~20% failure rate on /residents, but
 * checkHealth() uses /health which has no failure simulation.
 * The fetchResidents test may occasionally fail due to simulated
 * 500 errors — this is expected behavior and will be handled
 * with retry logic in Phase 1.
 */
@SpringBootTest
class XmlSourceAdapterTest {

    @Autowired
    private XmlSourceAdapter xmlSourceAdapter;

    @Test
    void healthCheck_returnsTrue_whenMockServiceIsRunning() {
        boolean healthy = xmlSourceAdapter.checkHealth();
        assertTrue(healthy, "XML mock service should be reachable on port 3002. "
                + "Start it with: cd mock-services/xml-source && npm start");
    }

    @Test
    void fetchResidents_returnsXmlWithResidents_whenMockServiceIsRunning() {
        // The XML mock has ~20% failure rate; retry a few times for Phase 0
        String response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                response = xmlSourceAdapter.fetchResidents();
                if (response != null && response.contains("<residents")) {
                    break; // Success
                }
            } catch (Exception e) {
                lastException = e;
                // Expected — the mock intermittently returns 500s
            }
        }

        assertNotNull(response,
                "Should eventually get a response from XML source (tried 5 times). "
                        + (lastException != null ? "Last error: " + lastException.getMessage() : ""));

        assertTrue(response.contains("<?xml"),
                "Response should be XML");
        assertTrue(response.contains("<residents"),
                "Response should contain <residents> root element");
        assertTrue(response.contains("<firstName>"),
                "Response should contain resident data");
    }
}
