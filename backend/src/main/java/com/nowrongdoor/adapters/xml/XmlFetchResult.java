package com.nowrongdoor.adapters.xml;

import java.util.List;

/**
 * Sealed result returned by {@link XmlSourceAdapter#fetchAllRecords()}.
 * <p>
 * The adapter never throws — callers pattern-match on Success vs Failure.
 */
public sealed interface XmlFetchResult
        permits XmlFetchResult.Success, XmlFetchResult.Failure {

    /**
     * @param records  list of benefit records parsed from the XML source
     * @param attempts number of HTTP attempts made (1 = no retry needed)
     */
    record Success(List<XmlRecord> records, int attempts) implements XmlFetchResult {}

    /**
     * @param message  human-readable description of the failure
     * @param attempts number of HTTP attempts made before giving up
     * @param cause    underlying exception from the last attempt, or {@code null}
     */
    record Failure(String message, int attempts, Exception cause) implements XmlFetchResult {
        public Failure(String message, int attempts) { this(message, attempts, null); }
    }
}
