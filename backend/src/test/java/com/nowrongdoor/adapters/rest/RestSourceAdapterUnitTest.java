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
 * Unit tests for {@link RestSourceAdapter} — official contract.
 * <p>
 * Uses WireMock to simulate the REST Resident Index. No live service required.
 * <p>
 * Covers:
 * <ul>
 *   <li>Official JSON envelope: {@code page}, {@code page_size}, {@code total}, {@code has_more}, {@code results}</li>
 *   <li>Official resident fields: {@code id}, {@code first_name}, {@code last_name}, {@code date_of_birth},
 *       {@code address_line}, {@code city}, {@code phone}, {@code program_status}, {@code last_contact}</li>
 *   <li>First page only (has_more = false)</li>
 *   <li>Multiple pages walked via has_more</li>
 *   <li>Duplicate records across pages deduplicated by {@code id} (first-seen wins)</li>
 *   <li>Empty results from first page</li>
 *   <li>Source unreachable → Failure</li>
 *   <li>Malformed JSON → Failure</li>
 *   <li>Deterministic insertion order</li>
 * </ul>
 */
class RestSourceAdapterUnitTest {

    private WireMockServer wireMock;
    private RestSourceAdapter adapter;

    /**
     * Build the official JSON envelope for one page.
     *
     * @param page    current page number
     * @param hasMore whether more pages follow
     * @param total   total records in source (informational)
     * @param records JSON objects for the results array
     */
    private static String pageJson(int page, boolean hasMore, int total, String... records) {
        StringBuilder results = new StringBuilder("[");
        for (int i = 0; i < records.length; i++) {
            if (i > 0) results.append(',');
            results.append(records[i]);
        }
        results.append("]");

        return "{"
               + "\"page\":" + page + ","
               + "\"page_size\":25,"
               + "\"total\":" + total + ","
               + "\"has_more\":" + hasMore + ","
               + "\"results\":" + results
               + "}";
    }

    /**
     * Build a single resident JSON object using the official REST field names.
     */
    private static String resident(String id, String firstName, String lastName) {
        return "{\"id\":\"" + id + "\","
               + "\"first_name\":\"" + firstName + "\","
               + "\"last_name\":\"" + lastName + "\","
               + "\"date_of_birth\":\"1990-01-01\","
               + "\"address_line\":\"123 Main St\","
               + "\"city\":\"Testville\","
               + "\"phone\":\"555-0100\","
               + "\"program_status\":\"Active\","
               + "\"last_contact\":\"2025-01-01\"}";
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        RestSourceConfig config = new RestSourceConfig();
        config.setBaseUrl("http://localhost:" + wireMock.port());
        config.setPageSize(25);

        adapter = new RestSourceAdapter(new RestTemplate(), config, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    // ── 1. Single page, has_more = false ──────────────────────────────────

    @Test
    void fetchAllResidents_singlePage_returnsAllRecords() {
        String r1 = resident("R-001", "Maria", "Garcia");
        String r2 = resident("R-002", "James", "Johnson");

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, false, 2, r1, r2))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();
        assertEquals(2, residents.size());
        assertEquals("R-001", residents.get(0).getId());
        assertEquals("R-002", residents.get(1).getId());

        // Only page 1 should have been requested
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/residents")));
    }

    // ── 2. Multiple pages — has_more drives pagination ────────────────────

    @Test
    void fetchAllResidents_multiplePages_followsHasMore() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, true, 5,
                                resident("R-001", "Alice", "A"),
                                resident("R-002", "Bob", "B")))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, true, 5,
                                resident("R-003", "Carol", "C"),
                                resident("R-004", "Dave", "D")))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(3, false, 5,
                                resident("R-005", "Eve", "E")))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();
        assertEquals(5, residents.size());

        // All three pages must have been requested
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3")));
        // Page 4 must NOT have been requested
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("4")));
    }

    // ── 3. Duplicate across pages — deduplication by id ───────────────────

    @Test
    void fetchAllResidents_duplicateAcrossPages_deduplicatedById() {
        String r1 = resident("R-001", "Maria",  "Garcia");
        String r2 = resident("R-002", "James",  "Johnson");
        String r3 = resident("R-003", "Aisha",  "Patel");

        // Page 1: R-001, R-002  |  Page 2: R-002 (duplicate!), R-003
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, true, 3, r1, r2))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, false, 3, r2, r3))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        List<RestResident> residents = ((RestFetchResult.Success) result).residents();

        assertEquals(3, residents.size(),
                "R-002 appeared on both pages but must be present exactly once");

        long countR002 = residents.stream()
                .filter(r -> "R-002".equals(r.getId()))
                .count();
        assertEquals(1, countR002, "R-002 must appear exactly once");

        // First-seen wins: insertion order is R-001, R-002, R-003
        assertEquals("R-001", residents.get(0).getId());
        assertEquals("R-002", residents.get(1).getId());
        assertEquals("R-003", residents.get(2).getId());
    }

    // ── 4. duplicatesDropped counter ──────────────────────────────────────

    @Test
    void fetchAllResidents_duplicateAcrossPages_duplicatesDroppedIsCorrect() {
        String r1 = resident("R-001", "Alpha", "A");
        String r2 = resident("R-002", "Beta",  "B");

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, true, 2, r1, r2))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, false, 2, r1, r2)))); // both duplicated

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        RestFetchResult.Success success = (RestFetchResult.Success) result;
        assertEquals(2, success.residents().size());
        assertEquals(2, success.duplicatesDropped(),
                "Both R-001 and R-002 appeared on page 2 as duplicates");
    }

    // ── 5. Empty first page ───────────────────────────────────────────────

    @Test
    void fetchAllResidents_emptySource_returnsEmptyList() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, false, 0))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        assertTrue(((RestFetchResult.Success) result).residents().isEmpty());
    }

    // ── 6. Stops on has_more = false, not on total ────────────────────────

    @Test
    void fetchAllResidents_stopsWhenHasMoreFalse_notBasedOnTotal() {
        // total says 4 records across 4 pages, but has_more goes false after page 2
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, true, 4, resident("R-001", "A", "A")))));

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(2, false, 4, resident("R-002", "B", "B")))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        assertEquals(2, ((RestFetchResult.Success) result).residents().size());

        // Page 3 must NOT have been fetched even though total=4
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("3")));
    }

    // ── 7. Source unreachable ─────────────────────────────────────────────

    @Test
    void fetchAllResidents_sourceUnreachable_returnsFailure() {
        wireMock.stop(); // port is no longer listening

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Failure.class, result);
        assertFalse(((RestFetchResult.Failure) result).message().isBlank());
    }

    // ── 8. Malformed JSON → Failure ───────────────────────────────────────

    @Test
    void fetchAllResidents_malformedJson_returnsFailure() {
        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("this is not json !!!")));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Failure.class, result);
    }

    // ── 9. Deterministic output for identical fixtures ────────────────────

    @Test
    void fetchAllResidents_sameFixture_producesDeterministicOrder() {
        String body = pageJson(1, false, 3,
                resident("R-010", "Priya",  "Sharma"),
                resident("R-001", "Maria",  "Garcia"),
                resident("R-005", "Chen",   "Wei"));

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

    // ── 10. All official REST fields are correctly parsed ─────────────────

    @Test
    void fetchAllResidents_parsesAllOfficialFields() {
        String rec = "{\"id\":\"R-10394\","
                   + "\"first_name\":\"Paul\","
                   + "\"last_name\":\"Quill\","
                   + "\"date_of_birth\":\"1955-06-10\","
                   + "\"address_line\":\"261 Sycamore Dr\","
                   + "\"city\":\"Weybridge\","
                   + "\"phone\":\"555-375-2897\","
                   + "\"program_status\":\"Suspended\","
                   + "\"last_contact\":\"2025-04-07\"}";

        wireMock.stubFor(get(urlPathEqualTo("/residents"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson(1, false, 1, rec))));

        RestFetchResult result = adapter.fetchAllResidents();

        assertInstanceOf(RestFetchResult.Success.class, result);
        RestResident r = ((RestFetchResult.Success) result).residents().get(0);

        assertEquals("R-10394",        r.getId());
        assertEquals("Paul",           r.getFirstName());
        assertEquals("Quill",          r.getLastName());
        assertEquals("1955-06-10",     r.getDateOfBirth());
        assertEquals("261 Sycamore Dr",r.getAddressLine());
        assertEquals("Weybridge",      r.getCity());
        assertEquals("555-375-2897",   r.getPhone());
        assertEquals("Suspended",      r.getProgramStatus());
        assertEquals("2025-04-07",     r.getLastContact());
    }
}
