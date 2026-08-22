package com.nowrongdoor.adapters.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RestSourceAdapter} — pagination walking and deduplication.
 * <p>
 * Uses WireMock to simulate the REST source HTTP server, so these tests run
 * without the live Node.js mock service. Tests cover all key Phase 1 behaviours.
 */
class RestSourceAdapterUnitTest {

    private WireMockServer wireMock;
    private RestSourceAdapter adapter;

    /** Minimal one-page JSON response with a given page/totalPages. */
    private static String pageJson(int page, int totalPages, String... records) {
        StringBuilder data = new StringBuilder("[");
        for (int i = 0; i < records.length; i++) {
            if (i > 0) data.append(',');
            data.append(records[i]);
        }
        data.append("]");

        return "{"
               + "\"page\":" + page + ","
               + "\"size\":10,"
               + "\"totalPages\":" + totalPages + ","
               + "\"totalRecords\":" + (totalPages * records.length) + ","
               + "\"data\":" + data
               + "}";
    }

    /** JSON for a single resident object. */
    private static String resident(String id, String first, String last) {
        return "{\"id\":\"" + id + "\","
               + "\"firstName\":\"" + first + "\","
               + "\"lastName\":\"" + last + "\","
               + "\"dateOfBirth\":\"1990-01-01\","
               + "\"address\":\"123 Main St\","
               + "\"phone\":\"555-0100\"}";
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        RestSourceConfig config = new RestSourceConfig();
        config.setBaseUrl("http://localhost:" + wireMock.port());
        config.setPageSize(10);

        adapter = new RestSourceAdapter(new RestTemplate(), config, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    // ── 1. Single page ─────────────────────────────────────────────────────

    @Test
    void fetchAllResidents_singlePage_returnsAllRecords() {
        String r1 = resident("R001", "Maria", "Garcia");
        String r2 = resident("R002", "James", "Johnson");

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, 1, r1, r2))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();
        assertEquals(2, residents.size());
        assertEquals("R001", residents.get(0).getId());
        assertEquals("R002", residents.get(1).getId());
    }

    // ── 2. Multiple pages ──────────────────────────────────────────────────

    @Test
    void fetchAllResidents_multiplePages_fetchesAllPages() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, 3,
                                resident("R001", "Alice", "A"),
                                resident("R002", "Bob", "B")))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, 3,
                                resident("R003", "Carol", "C"),
                                resident("R004", "Dave", "D")))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(3, 3,
                                resident("R005", "Eve", "E")))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();
        assertEquals(5, residents.size());

        // Verify all three pages were actually requested
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3")));
    }

    // ── 3. Duplicate across pages — deduplicated ───────────────────────────

    @Test
    void fetchAllResidents_duplicateAcrossPages_appearsOnce() {
        String r1 = resident("R001", "Maria", "Garcia");
        String r2 = resident("R002", "James", "Johnson");
        String r3 = resident("R003", "Aisha", "Patel");

        // Page 1: R001, R002
        // Page 2: R002 (duplicate!), R003
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, 2, r1, r2))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, 2, r2, r3))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();

        // R002 appeared twice (page 1 and page 2) but must be present only once
        assertEquals(3, residents.size(), "Expected 3 unique residents; duplicate must be removed");

        long countR002 = residents.stream()
                .filter(r -> "R002".equals(r.getId()))
                .count();
        assertEquals(1, countR002, "R002 must appear exactly once");

        // The page-1 copy is kept (first-seen-wins): verify order R001, R002, R003
        assertEquals("R001", residents.get(0).getId());
        assertEquals("R002", residents.get(1).getId());
        assertEquals("R003", residents.get(2).getId());
    }

    // ── 4. Empty result ────────────────────────────────────────────────────

    @Test
    void fetchAllResidents_emptySource_returnsEmptyList() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"page\":1,\"size\":10,\"totalPages\":1,"
                                + "\"totalRecords\":0,\"data\":[]}")));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        assertTrue(((RestFetchResult.Success) result).residents().isEmpty());
    }

    // ── 5. Pagination metadata — page size variation ───────────────────────

    @Test
    void fetchAllResidents_readsPageCountFromMetadata_notHardcoded() {
        // Server returns 4 pages — adapter must NOT stop at 2 or any fixed number
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, 4, resident("R001", "A", "A")))));
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, 4, resident("R002", "B", "B")))));
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(3, 4, resident("R003", "C", "C")))));
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("4"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(4, 4, resident("R004", "D", "D")))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        assertEquals(4, ((RestFetchResult.Success) result).residents().size());

        // All 4 pages must have been requested
        for (int p = 1; p <= 4; p++) {
            wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                    .withQueryParam("page", equalTo(String.valueOf(p))));
        }
    }

    // ── 6. REST source unreachable ─────────────────────────────────────────

    @Test
    void fetchAllResidents_sourceUnreachable_returnsFailure() {
        // Stop WireMock before the call so the port is not listening
        wireMock.stop();

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Failure.class, result);
        assertFalse(((RestFetchResult.Failure) result).message().isBlank());
    }

    // ── 7. Malformed/unexpected response ──────────────────────────────────

    @Test
    void fetchAllResidents_malformedJson_returnsFailure() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("this is not json at all !!!")));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Failure.class, result);
    }

    // ── 8. Determinism — same fixture produces same ordered output ─────────

    @Test
    void fetchAllResidents_sameFikture_producesDeterministicOrder() {
        String body = pageJson(1, 1,
                resident("R010", "Priya",   "Sharma"),
                resident("R001", "Maria",   "Garcia"),
                resident("R005", "Chen",    "Wei"));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        RestFetchResult result1 = adapter.fetchAllResidents();

        // Re-stub identically and call again
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        RestFetchResult result2 = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result1);
        assertInstanceOf(RestFetchResult.Success.class, result2);

        List<RestResident> r1 = ((RestFetchResult.Success) result1).residents();
        List<RestResident> r2 = ((RestFetchResult.Success) result2).residents();

        assertEquals(r1.size(), r2.size());
        for (int i = 0; i < r1.size(); i++) {
            assertEquals(r1.get(i).getId(), r2.get(i).getId(),
                    "Order at index " + i + " differs between two identical fetches");
        }
    }
}
