package com.nowrongdoor.api;

import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import com.nowrongdoor.adapters.xml.XmlSourceAdapter;
import com.nowrongdoor.ingestion.IngestionResult;
import com.nowrongdoor.ingestion.IngestionService;
import com.nowrongdoor.model.IngestionRun;
import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.IngestionRunRepository;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({StatusController.class, IngestionController.class})
class StatusAndIngestionControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private RestSourceAdapter restAdapter;
    @MockBean private XmlSourceAdapter xmlAdapter;
    @MockBean private UnifiedResidentRepository residents;
    @MockBean private IngestionRunRepository runs;
    @MockBean private IngestionService ingestionService;

    @Test
    void dashboardStatisticsComeFromDatabaseAndLatestRun() throws Exception {
        IngestionRun run = new IngestionRun().startedAt(Instant.parse("2026-08-22T10:00:00Z"))
                .finishedAt(Instant.parse("2026-08-22T10:01:00Z"))
                .status(IngestionRun.Status.SUCCESS);
        when(residents.count()).thenReturn(620L);
        when(residents.countByMatchStatus(MatchStatus.EXACT)).thenReturn(306L);
        when(residents.countByMatchStatus(MatchStatus.PROBABLE)).thenReturn(34L);
        when(residents.countByMatchStatus(MatchStatus.AMBIGUOUS)).thenReturn(7L);
        when(residents.countByMatchStatus(MatchStatus.REST_ONLY)).thenReturn(273L);
        when(residents.countByMatchStatus(MatchStatus.XML_ONLY)).thenReturn(200L);
        when(runs.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(run));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResidents").value(620))
                .andExpect(jsonPath("$.exact").value(306))
                .andExpect(jsonPath("$.lastIngestionStatus").value("SUCCESS"));
    }

    @Test
    void ingestionEndpointReturnsServiceResult() throws Exception {
        when(ingestionService.ingest()).thenReturn(new IngestionResult(1L, IngestionRun.Status.PARTIAL,
                620, 0, 0, 0, 0, 620, 0, 1000, "XML unavailable"));

        mvc.perform(post("/api/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.restRecords").value(620))
                .andExpect(jsonPath("$.error").value("XML unavailable"));
        verify(ingestionService).ingest();
    }
}
