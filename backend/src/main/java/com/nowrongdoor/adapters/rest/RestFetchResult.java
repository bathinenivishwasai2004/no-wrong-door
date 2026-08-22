package com.nowrongdoor.adapters.rest;

import java.util.List;

/**
 * Sealed result returned by {@link RestSourceAdapter#fetchAllResidents()}.
 * Never throws — callers pattern-match on Success vs Failure.
 */
public sealed interface RestFetchResult
        permits RestFetchResult.Success, RestFetchResult.Failure {

    /**
     * @param residents       deduplicated, ordered list of all residents
     * @param pagesFetched    number of pages walked
     * @param duplicatesDropped number of cross-page duplicate records discarded
     */
    record Success(List<RestResident> residents,
                   int pagesFetched,
                   int duplicatesDropped) implements RestFetchResult {}

    /**
     * @param message human-readable description of the failure
     * @param cause   underlying exception, or {@code null}
     */
    record Failure(String message, Exception cause) implements RestFetchResult {
        public Failure(String message) { this(message, null); }
    }
}
