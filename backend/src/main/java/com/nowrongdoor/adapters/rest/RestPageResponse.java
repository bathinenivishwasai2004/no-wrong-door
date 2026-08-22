package com.nowrongdoor.adapters.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * Represents one page of the REST source response envelope.
 * <p>
 * The REST source returns JSON in this shape:
 * <pre>
 * {
 *   "page":         1,
 *   "size":         10,
 *   "totalPages":   2,
 *   "totalRecords": 20,
 *   "data": [ ... ]
 * }
 * </pre>
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} guards against future
 * fields being added to the source API without breaking deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestPageResponse {

    private int page;
    private int size;
    private int totalPages;
    private int totalRecords;
    private List<RestResident> data = Collections.emptyList();

    /** Required by Jackson. */
    public RestPageResponse() {}

    // ── Accessors ──────────────────────────────────────────────────────────

    public int getPage()          { return page; }
    public int getSize()          { return size; }
    public int getTotalPages()    { return totalPages; }
    public int getTotalRecords()  { return totalRecords; }

    public List<RestResident> getData() {
        return data != null ? data : Collections.emptyList();
    }

    // Setters required by Jackson
    public void setPage(int page)               { this.page = page; }
    public void setSize(int size)               { this.size = size; }
    public void setTotalPages(int totalPages)   { this.totalPages = totalPages; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public void setData(List<RestResident> data)  { this.data = data; }

    @Override
    public String toString() {
        return "RestPageResponse{page=" + page + ", totalPages=" + totalPages +
               ", records=" + (data != null ? data.size() : 0) + "}";
    }
}
