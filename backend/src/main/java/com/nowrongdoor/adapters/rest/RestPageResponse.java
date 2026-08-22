package com.nowrongdoor.adapters.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * One page of the official REST Resident Index paginated response.
 * <p>
 * The REST source returns:
 * <pre>
 * {
 *   "page":      1,
 *   "page_size": 25,
 *   "total":     620,
 *   "has_more":  true,
 *   "results":   [ ... ]
 * }
 * </pre>
 * <p>
 * {@code has_more} is the authoritative signal to keep paginating.
 * Do NOT derive a page count from {@code total} — the service has
 * unstable boundaries and {@code has_more} is the source of truth.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestPageResponse {

    private int page;

    @JsonProperty("page_size")
    private int pageSize;

    /** Total number of records in the source (informational only — do not drive pagination from this). */
    private int total;

    /** {@code true} if there is at least one more page to fetch. */
    @JsonProperty("has_more")
    private boolean hasMore;

    /** The records on this page. */
    private List<RestResident> results = Collections.emptyList();

    public RestPageResponse() {}

    public int getPage()       { return page; }
    public int getPageSize()   { return pageSize; }
    public int getTotal()      { return total; }
    public boolean isHasMore() { return hasMore; }
    public List<RestResident> getResults() {
        return results != null ? results : Collections.emptyList();
    }

    public void setPage(int v)                       { this.page = v; }
    public void setPageSize(int v)                   { this.pageSize = v; }
    public void setTotal(int v)                      { this.total = v; }
    public void setHasMore(boolean v)                { this.hasMore = v; }
    public void setResults(List<RestResident> v)     { this.results = v; }

    @Override
    public String toString() {
        return "RestPageResponse{page=" + page + ", hasMore=" + hasMore
               + ", results=" + (results != null ? results.size() : 0) + "}";
    }
}
