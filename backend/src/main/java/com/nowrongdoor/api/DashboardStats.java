package com.nowrongdoor.api;

import com.nowrongdoor.model.IngestionRun;
import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.repository.IngestionRunRepository;
import com.nowrongdoor.repository.UnifiedResidentRepository;

import java.time.Instant;

public record DashboardStats(
        long totalResidents,
        long exact,
        long probable,
        long ambiguous,
        long restOnly,
        long xmlOnly,
        IngestionRun.Status lastIngestionStatus,
        String lastIngestionTime,
        Long lastIngestionRunId
) {
    public static DashboardStats from(UnifiedResidentRepository residents,
                                      IngestionRunRepository runs) {
        var latest = runs.findTopByOrderByStartedAtDesc().orElse(null);
        return new DashboardStats(
                residents.count(),
                residents.countByMatchStatus(MatchStatus.EXACT),
                residents.countByMatchStatus(MatchStatus.PROBABLE),
                residents.countByMatchStatus(MatchStatus.AMBIGUOUS),
                residents.countByMatchStatus(MatchStatus.REST_ONLY),
                residents.countByMatchStatus(MatchStatus.XML_ONLY),
                latest == null ? null : latest.getStatus(),
                latest == null || latest.getFinishedAt() == null ? null : latest.getFinishedAt().toString(),
                latest == null ? null : latest.getId());
    }
}
