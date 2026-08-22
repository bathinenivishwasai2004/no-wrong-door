package com.nowrongdoor.ingestion;

import com.nowrongdoor.adapters.rest.RestFetchResult;
import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import com.nowrongdoor.adapters.xml.XmlFetchResult;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.adapters.xml.XmlSourceAdapter;
import com.nowrongdoor.matching.MatchingService;
import com.nowrongdoor.model.IngestionRun;
import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.IngestionRunRepository;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class IngestionPersistenceIntegrationTest {
    @MockBean private RestSourceAdapter restAdapter;
    @MockBean private XmlSourceAdapter xmlAdapter;
    @MockBean private MatchingService matchingService;

    @Autowired private IngestionService service;
    @Autowired private UnifiedResidentRepository residentRepository;
    @Autowired private IngestionRunRepository runRepository;

    @Test
    void persistsSourceColumnsAuditAndUpdatesSameNaturalKeyOnRepeat() {
        RestResident rest = rest("R-1");
        XmlRecord xml = xml("X-1");
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Success(List.of(rest), 1, 0));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(xml), 1));
        when(matchingService.match(anyList(), anyList())).thenReturn(List.of(
                new com.nowrongdoor.matching.MatchResult(rest, xml, MatchStatus.EXACT, 100, "exact",
                        new com.nowrongdoor.matching.MatchEvidence(true, true, true, false, false, List.of("X-1")))));

        service.ingest();
        service.ingest();

        assertEquals(1, residentRepository.count());
        UnifiedResident saved = residentRepository.findByRestId("R-1").orElseThrow();
        assertEquals("R-1", saved.getRestId());
        assertEquals("X-1", saved.getXmlRef());
        assertEquals("555-1", saved.getRestPhone());
        assertEquals("BEN-1", saved.getXmlBenefitCode());
        assertEquals(MatchStatus.EXACT, saved.getMatchStatus());
        assertEquals(2, runRepository.count());
        assertEquals(IngestionRun.Status.SUCCESS, runRepository.findTopByOrderByStartedAtDesc().orElseThrow().getStatus());
    }

    private static RestResident rest(String id) {
        RestResident value = new RestResident();
        value.setId(id); value.setFirstName("Jane"); value.setLastName("Doe"); value.setDateOfBirth("1980-01-01");
        value.setAddressLine("1 Main St"); value.setCity("Town"); value.setPhone("555-1");
        value.setProgramStatus("Active"); value.setLastContact("2025-01-01");
        return value;
    }

    private static XmlRecord xml(String ref) {
        XmlRecord value = new XmlRecord();
        value.setRef(ref); value.setName("DOE, Jane"); value.setBorn("1980-01-01");
        value.setAddr("1 Main St"); value.setTown("Town"); value.setBenefitCode("BEN-1"); value.setReviewDue("2026-01-01");
        return value;
    }
}
