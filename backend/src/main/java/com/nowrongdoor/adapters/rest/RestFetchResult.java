package com.nowrongdoor.adapters.rest;

import java.util.List;

/**
 * Sealed result type returned by {@link RestSourceAdapter#fetchAllResidents()}.
 * <p>
 * The adapter never throws — it returns either a {@link Success} containing the
 * full deduplicated resident list, or a {@link Failure} containing a human-readable
 * message and the original exception.
 * <p>
 * This allows the calling controller to decide the appropriate HTTP response without
 * catching exceptions itself, and prepares the codebase for Phase 2 graceful-degradation
 * logic (e.g., continue with XML source even when REST fails).
 */
public sealed interface RestFetchResult
        permits RestFetchResult.Success, RestFetchResult.Failure {

    /**
     * The REST fetch completed successfully.
     *
     * @param residents deduplicated, ordered list of residents across all pages
     */
    record Success(List<RestResident> residents) implements RestFetchResult {}

    /**
     * The REST fetch failed — source unreachable, unexpected HTTP status,
     * or malformed response.
     *
     * @param message  human-readable description of the failure
     * @param cause    the underlying exception, or {@code null} if not applicable
     */
    record Failure(String message, Exception cause) implements RestFetchResult {

        /** Convenience constructor when no underlying exception is available. */
        public Failure(String message) {
            this(message, null);
        }
    }
}
