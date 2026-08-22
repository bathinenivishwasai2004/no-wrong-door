package com.nowrongdoor.adapters.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link XmlSourceAdapter} — official XML contract.
 * <p>
 * Uses WireMock to simulate the Benefits Register (XML source). No live service required.
 * <p>
 * Covers:
 * <ul>
 *   <li>{@code <BenefitsRegister>} root with multiple {@code <Record>} elements</li>
 *   <li>All seven official XML fields: Ref, Name, Born, Addr, Town, BenefitCode, ReviewDue</li>
 *   <li>HTTP 500 → retry → eventual success</li>
 *   <li>HTTP 500 for all attempts → Failure with correct attempt count</li>
 *   <li>Connection/timeout failure → retry → Failure</li>
 *   <li>Successful parse on first attempt (no retries needed)</li>
 * </ul>
 */
class XmlSourceAdapterUnitTest {

    private WireMockServer wireMock;
    private XmlSourceAdapter adapter;

    /** Build a full BenefitsRegister XML document with the given Record blocks. */
    private static String benefitsRegisterXml(String... records) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<BenefitsRegister>\n");
        for (String record : records) {
            sb.append(record);
        }
        sb.append("</BenefitsRegister>");
        return sb.toString();
    }

    /**
     * Build a single {@code <Record>} block using the official XML field names.
     */
    private static String record(String ref, String name, String born,
                                  String addr, String town,
                                  String benefitCode, String reviewDue) {
        return "  <Record>\n"
             + "    <Ref>" + ref + "</Ref>\n"
             + "    <Name>" + name + "</Name>\n"
             + "    <Born>" + born + "</Born>\n"
             + "    <Addr>" + addr + "</Addr>\n"
             + "    <Town>" + town + "</Town>\n"
             + "    <BenefitCode>" + benefitCode + "</BenefitCode>\n"
             + "    <ReviewDue>" + reviewDue + "</ReviewDue>\n"
             + "  </Record>\n";
    }

    private static final String FAULT_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<Fault><Code>SRV-500</Code>"
            + "<Message>Register temporarily unavailable. Retry.</Message></Fault>";

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        XmlSourceConfig config = new XmlSourceConfig();
        config.setBaseUrl("http://localhost:" + wireMock.port());
        config.setMaxRetries(3);
        config.setInitialBackoffMs(1);   // minimal backoff for unit tests
        config.setTimeoutMs(5000);

        adapter = new XmlSourceAdapter(new RestTemplate(), config, new XmlMapper());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    // ── 1. BenefitsRegister root with single Record ────────────────────────

    @Test
    void fetchAllRecords_singleRecord_parsedCorrectly() {
        String xml = benefitsRegisterXml(
                record("AS/2024/4702", "EASTWOOD, Donna", "1973-11-18",
                       "137 Poplar Road", "Ash Hill", "TRN-1", "2026-06-25"));

        wireMock.stubFor(get(urlEqualTo("/records"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody(xml)));

        XmlFetchResult result = adapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Success.class, result);
        List<XmlRecord> records = ((XmlFetchResult.Success) result).records();
        assertEquals(1, records.size());
        assertEquals(1, ((XmlFetchResult.Success) result).attempts());
    }

    // ── 2. All seven official XML fields correctly parsed ─────────────────

    @Test
    void fetchAllRecords_allSevenFieldsParsedCorrectly() {
        String xml = benefitsRegisterXml(
                record("AS/2024/4702", "EASTWOOD, Donna", "1973-11-18",
                       "137 Poplar Road", "Ash Hill", "TRN-1", "2026-06-25"));

        wireMock.stubFor(get(urlEqualTo("/records"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody(xml)));

        XmlFetchResult result = adapter.fetchAllRecords();
        assertInstanceOf(XmlFetchResult.Success.class, result);

        XmlRecord r = ((XmlFetchResult.Success) result).records().get(0);
        assertEquals("AS/2024/4702",    r.getRef());
        assertEquals("EASTWOOD, Donna", r.getName());
        assertEquals("1973-11-18",      r.getBorn());
        assertEquals("137 Poplar Road", r.getAddr());
        assertEquals("Ash Hill",        r.getTown());
        assertEquals("TRN-1",           r.getBenefitCode());
        assertEquals("2026-06-25",      r.getReviewDue());
    }

    // ── 3. Multiple Record elements ────────────────────────────────────────

    @Test
    void fetchAllRecords_multipleRecords_allParsed() {
        String xml = benefitsRegisterXml(
                record("REF-001", "SMITH, John",  "1980-05-15", "1 Oak St", "Maplewood", "HSG-2", "2026-03-01"),
                record("REF-002", "DOE, Jane",    "1975-11-20", "2 Elm Ave", "Riverside", "TRN-1", "2026-07-15"),
                record("REF-003", "JONES, Robert","1990-08-07", "3 Pine Rd", "Lakewood",  "MED-3", "2025-12-31"));

        wireMock.stubFor(get(urlEqualTo("/records"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody(xml)));

        XmlFetchResult result = adapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Success.class, result);
        List<XmlRecord> records = ((XmlFetchResult.Success) result).records();
        assertEquals(3, records.size());
        assertEquals("REF-001", records.get(0).getRef());
        assertEquals("REF-002", records.get(1).getRef());
        assertEquals("REF-003", records.get(2).getRef());
    }

    // ── 4. HTTP 500 on first attempt, success on second ───────────────────

    @Test
    void fetchAllRecords_http500ThenSuccess_retriesAndSucceeds() {
        String xml = benefitsRegisterXml(
                record("REF-001", "SMITH, John", "1980-05-15",
                       "1 Oak St", "Maplewood", "HSG-2", "2026-03-01"));

        wireMock.stubFor(get(urlEqualTo("/records"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/xml")
                        .withBody(FAULT_XML))
                .willSetStateTo("retry-1"));

        wireMock.stubFor(get(urlEqualTo("/records"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("retry-1")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody(xml)));

        XmlFetchResult result = adapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Success.class, result,
                "Expected success after retry, got: " + result);
        XmlFetchResult.Success success = (XmlFetchResult.Success) result;
        assertEquals(1, success.records().size());
        assertEquals(2, success.attempts(),
                "Should have taken exactly 2 attempts (1 failure + 1 success)");
    }

    // ── 5. All attempts exhausted → Failure ───────────────────────────────

    @Test
    void fetchAllRecords_allAttemptsExhausted_returnsFailure() {
        wireMock.stubFor(get(urlEqualTo("/records"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/xml")
                        .withBody(FAULT_XML)));

        XmlFetchResult result = adapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Failure.class, result);
        XmlFetchResult.Failure failure = (XmlFetchResult.Failure) result;
        assertFalse(failure.message().isBlank());
        // maxRetries = 3 configured in setUp()
        assertEquals(3, failure.attempts(),
                "Should have made exactly 3 attempts before giving up");
    }

    // ── 6. Connection error → retries → Failure ───────────────────────────

    @Test
    void fetchAllRecords_connectionError_retriesAndEventuallyFails() {
        // Stop WireMock so the port is not listening — simulates connection refused
        wireMock.stop();

        XmlFetchResult result = adapter.fetchAllRecords();

        assertInstanceOf(XmlFetchResult.Failure.class, result);
        XmlFetchResult.Failure failure = (XmlFetchResult.Failure) result;
        assertFalse(failure.message().isBlank());
    }

    // ── 7. Health endpoint has no latency/failure simulation ──────────────

    @Test
    void checkHealth_returnsTrue_whenServiceUp() {
        wireMock.stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\"?><Health><Status>ok</Status></Health>")));

        assertTrue(adapter.checkHealth());
    }

    @Test
    void checkHealth_returnsFalse_whenServiceDown() {
        wireMock.stop();
        assertFalse(adapter.checkHealth());
    }
}
