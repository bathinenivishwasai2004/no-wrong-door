package com.nowrongdoor.ingestion;

import com.nowrongdoor.adapters.rest.RestFetchResult;
import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import com.nowrongdoor.adapters.xml.XmlFetchResult;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.adapters.xml.XmlSourceAdapter;
import com.nowrongdoor.matching.MatchEvidence;
import com.nowrongdoor.matching.MatchResult;
import com.nowrongdoor.matching.MatchingService;
import com.nowrongdoor.model.IngestionRun;
import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.IngestionRunRepository;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngestionServiceTest {
    private RestSourceAdapter restAdapter;
    private XmlSourceAdapter xmlAdapter;
    private MatchingService matchingService;
    private UnifiedResidentRepository residentRepository;
    private IngestionRunRepository runRepository;
    private IngestionService service;

    @BeforeEach
    void setUp() {
        restAdapter = mock(RestSourceAdapter.class);
        xmlAdapter = mock(XmlSourceAdapter.class);
        matchingService = mock(MatchingService.class);
        residentRepository = mock(UnifiedResidentRepository.class);
        runRepository = mock(IngestionRunRepository.class);
        when(runRepository.save(any(IngestionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residentRepository.save(any(UnifiedResident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residentRepository.findByRestId(anyString())).thenReturn(Optional.empty());
        when(residentRepository.findByXmlRef(anyString())).thenReturn(Optional.empty());
        service = new IngestionService(restAdapter, xmlAdapter, matchingService,
                residentRepository, runRepository);
    }

    @Test
    void successfulRunPersistsMatchesAndAuditCounts() {
        RestResident rest = rest("R-1", "Jane", "Doe");
        XmlRecord matchedXml = xml("X-1", "DOE, Jane");
        XmlRecord xmlOnly = xml("X-2", "SMITH, John");
        List<MatchResult> matches = List.of(
                match(rest, matchedXml, MatchStatus.EXACT, 100),
                match(rest("R-2", "Rest", "Only"), null, MatchStatus.REST_ONLY, 0),
                match(null, xmlOnly, MatchStatus.XML_ONLY, 0));
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Success(List.of(rest), 2, 1));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(matchedXml, xmlOnly), 1));
        when(matchingService.match(List.of(rest), List.of(matchedXml, xmlOnly))).thenReturn(matches);

        IngestionResult result = service.ingest();

        assertEquals(IngestionRun.Status.SUCCESS, result.status());
        assertEquals(1, result.exact());
        assertEquals(1, result.restOnly());
        assertEquals(1, result.xmlOnly());
        assertEquals(3, result.restRecords() + result.xmlRecords());
        var captured = org.mockito.ArgumentCaptor.forClass(UnifiedResident.class);
        verify(residentRepository, times(3)).save(captured.capture());
        assertTrue(captured.getAllValues().stream()
            .anyMatch(value -> value.getMatchStatus() == MatchStatus.REST_ONLY
                && value.getXmlRef() == null));
        assertTrue(captured.getAllValues().stream()
            .anyMatch(value -> value.getMatchStatus() == MatchStatus.XML_ONLY
                && value.getRestId() == null));
        verify(runRepository, times(2)).save(any(IngestionRun.class));
    }

    @Test
    void mappingPreservesBothSourceSides() {
        RestResident rest = rest("R-1", "Jane", "Doe");
        rest.setPhone("555-1"); rest.setProgramStatus("Active"); rest.setLastContact("2025-01-01");
        XmlRecord xml = xml("X-1", "DOE, Jane");
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Success(List.of(rest), 1, 0));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(xml), 1));
        when(matchingService.match(any(), any())).thenReturn(List.of(match(rest, xml, MatchStatus.EXACT, 100)));

        service.ingest();

        var captured = org.mockito.ArgumentCaptor.forClass(UnifiedResident.class);
        verify(residentRepository).save(captured.capture());
        UnifiedResident saved = captured.getValue();
        assertEquals("R-1", saved.getRestId());
        assertEquals("555-1", saved.getRestPhone());
        assertEquals("X-1", saved.getXmlRef());
        assertEquals("DOE, Jane", saved.getXmlName());
        assertEquals(MatchStatus.EXACT, saved.getMatchStatus());
        assertEquals(100, saved.getMatchConfidence());
        assertNotNull(saved.getMatchNotes());
        assertNotNull(saved.getIngestedAt());
    }

    @Test
    void xmlFailurePersistsRestOnlyAndMarksPartial() {
        RestResident rest = rest("R-1", "Jane", "Doe");
        RestFetchResult.Success restSuccess = new RestFetchResult.Success(List.of(rest), 1, 0);
        XmlFetchResult.Failure xmlFailure = new XmlFetchResult.Failure("XML unavailable", 3);
        when(restAdapter.fetchAllResidents()).thenReturn(restSuccess);
        when(xmlAdapter.fetchAllRecords()).thenReturn(xmlFailure);
        when(matchingService.match(List.of(rest), List.of())).thenReturn(
                List.of(match(rest, null, MatchStatus.REST_ONLY, 0)));

        IngestionResult result = service.ingest();

        assertEquals(IngestionRun.Status.PARTIAL, result.status());
        assertEquals(1, result.restRecords());
        assertEquals(0, result.xmlRecords());
        assertEquals(1, result.restOnly());
        assertTrue(result.error().contains("XML unavailable"));
        verify(matchingService).match(List.of(rest), List.of());
    }

    @Test
    void restFailurePersistsXmlOnlyAndMarksPartial() {
        XmlRecord xml = xml("X-1", "DOE, Jane");
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Failure("REST unavailable"));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(xml), 1));
        when(matchingService.match(List.of(), List.of(xml))).thenReturn(
                List.of(match(null, xml, MatchStatus.XML_ONLY, 0)));

        IngestionResult result = service.ingest();

        assertEquals(IngestionRun.Status.PARTIAL, result.status());
        assertEquals(1, result.xmlOnly());
        assertTrue(result.error().contains("REST unavailable"));
    }

    @Test
    void bothSourceFailuresMarkRunFailedWithoutMatching() {
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Failure("REST down"));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Failure("XML down", 3));
        when(matchingService.match(List.of(), List.of())).thenReturn(List.of());

        IngestionResult result = service.ingest();

        assertEquals(IngestionRun.Status.FAILED, result.status());
        assertEquals(0, result.exact() + result.probable() + result.ambiguous()
            + result.restOnly() + result.xmlOnly());
        assertTrue(result.error().contains("REST down"));
        assertTrue(result.error().contains("XML down"));
        verify(residentRepository, never()).save(any());
    }

    @Test
    void adapterExceptionIsConvertedToPartialAudit() {
        RestResident rest = rest("R-1", "Jane", "Doe");
        when(restAdapter.fetchAllResidents()).thenThrow(new IllegalStateException("network boom"));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(), 1));
        when(matchingService.match(List.of(), List.of())).thenReturn(List.of());

        IngestionResult result = service.ingest();

        assertEquals(IngestionRun.Status.PARTIAL, result.status());
        assertTrue(result.error().contains("network boom"));
    }

    @Test
    void repeatedRunUsesNaturalSourceKeyInsteadOfCreatingNewEntity() {
        RestResident rest = rest("R-1", "Jane", "Doe");
        XmlRecord xml = xml("X-1", "DOE, Jane");
        Map<String, UnifiedResident> byRest = new ConcurrentHashMap<>();
        when(residentRepository.findByRestId("R-1")).thenAnswer(invocation -> Optional.ofNullable(byRest.get("R-1")));
        when(residentRepository.save(any(UnifiedResident.class))).thenAnswer(invocation -> {
            UnifiedResident value = invocation.getArgument(0);
            byRest.put(value.getRestId(), value);
            return value;
        });
        when(restAdapter.fetchAllResidents()).thenReturn(new RestFetchResult.Success(List.of(rest), 1, 0));
        when(xmlAdapter.fetchAllRecords()).thenReturn(new XmlFetchResult.Success(List.of(xml), 1));
        when(matchingService.match(any(), any())).thenReturn(List.of(match(rest, xml, MatchStatus.EXACT, 100)));

        service.ingest();
        service.ingest();

        assertEquals(1, byRest.size());
        verify(residentRepository, times(2)).save(any(UnifiedResident.class));
    }

    private static MatchResult match(RestResident rest, XmlRecord xml, MatchStatus status, int confidence) {
        return new MatchResult(rest, xml, status, confidence, "test note",
                new MatchEvidence(xml != null, xml == null ? null : true, xml != null, xml == null, false,
                        xml == null ? List.of() : List.of(xml.getRef())));
    }

    private static RestResident rest(String id, String first, String last) {
        RestResident resident = new RestResident();
        resident.setId(id); resident.setFirstName(first); resident.setLastName(last);
        resident.setDateOfBirth("1980-01-01"); resident.setAddressLine("1 Main St"); resident.setCity("Town");
        return resident;
    }

    private static XmlRecord xml(String ref, String name) {
        XmlRecord record = new XmlRecord();
        record.setRef(ref); record.setName(name); record.setBorn("1980-01-01");
        record.setAddr("1 Main St"); record.setTown("Town");
        return record;
    }
}
