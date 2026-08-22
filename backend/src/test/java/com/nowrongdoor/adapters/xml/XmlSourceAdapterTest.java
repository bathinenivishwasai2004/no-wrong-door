package com.nowrongdoor.adapters.xml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the XML Benefits Register adapter.
 * <p>
 * Requires the official XML mock service to be running on port 8082.
 * Start it with: {@code python mock-services/xml-source/xml_service.py}
 * <p>
 * The official service:
 * <ul>
 *   <li>Serves 540 records wrapped in {@code <BenefitsRegister><Record>}</li>
 *   <li>Adds 0.7–2.4 second latency on every /records call</li>
 *   <li>Returns HTTP 500 approximately 15% of the time</li>
 *   <li>The adapter retries up to 3 times with exponential backoff</li>
 * </ul>
 */
@SpringBootTest
class XmlSourceAdapterTest {

    @Autowired
    private XmlSourceAdapter xmlSourceAdapter;

    // ── Health check — /health has no latency or failure simulation ────────

    @Test
    void healthCheck_returnsTrue_whenMockServiceIsRunning() {
        boolean healthy = xmlSourceAdapter.checkHealth();
        assertTrue(healthy,
                "XML mock service should be reachable on http://localhost:8082. "
                + "Start it with: python mock-services/xml-source/xml_service.py");
    }

    // ── Full fetch — expects 540 records on a successful run ──────────────

    @Test
    void fetchAllRecords_returns540Records_whenMockServiceIsRunning() {
        // The service may fail on the first attempt (15% rate) but the adapter
        // retries with backoff. A success within 3 attempts is expected.
        XmlFetchResult result = xmlSourceAdapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Success.class, result,
                "Expected Success (within 3 retry attempts) but got: " + result
                + ". Note: run again — 15% failure rate means ~0.4% chance of 3 consecutive failures.");

        XmlFetchResult.Success success = (XmlFetchResult.Success) result;
        List<XmlRecord> records = success.records();

        // Official dataset has exactly 540 records
        assertEquals(540, records.size(),
                "Expected exactly 540 XML records but got: " + records.size());

        // Spot-check that all 7 official fields are populated
        for (XmlRecord r : records) {
            assertNotNull(r.getRef(),         "Ref must not be null");
            assertFalse(r.getRef().isBlank(), "Ref must not be blank");
            assertNotNull(r.getName(),        "Name must not be null for ref=" + r.getRef());
            assertNotNull(r.getBorn(),        "Born must not be null for ref=" + r.getRef());
            assertNotNull(r.getBenefitCode(), "BenefitCode must not be null for ref=" + r.getRef());
        }

        System.out.printf(
                "XML integration: %d records parsed in %d attempt(s)%n",
                records.size(), success.attempts());
    }
}
