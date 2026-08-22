package com.nowrongdoor.ingestion;

import com.nowrongdoor.model.IngestionRun;

import java.time.Duration;

/** Public summary of one completed ingestion execution. */
public record IngestionResult(
        Long runId,
        IngestionRun.Status status,
        int restRecords,
        int xmlRecords,
        int exact,
        int probable,
        int ambiguous,
        int restOnly,
        int xmlOnly,
        long durationMs,
        String error
) {
    public static IngestionResult from(IngestionRun run) {
        long duration = run.getStartedAt() == null || run.getFinishedAt() == null
                ? 0
                : Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis();
        return new IngestionResult(
                run.getId(), run.getStatus(), run.getRestRecordsFetched(), run.getXmlRecordsFetched(),
                run.getExactMatches(), run.getProbableMatches(), run.getAmbiguousMatches(),
                run.getRestOnly(), run.getXmlOnly(), duration, run.getErrors());
    }
}
