package com.nowrongdoor.api;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResidentController.class)
class ResidentControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private UnifiedResidentRepository repository;

    @Test
    void searchReturnsDatabaseResidentsCaseInsensitively() throws Exception {
        when(repository.search("Ashley")).thenReturn(List.of(resident("R-1", "Ashley", "Stone", MatchStatus.EXACT)));

        mvc.perform(get("/api/residents/search").param("q", "Ashley"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.results[0].name").value("Ashley Stone"))
                .andExpect(jsonPath("$.results[0].rest.id").value("R-1"));
        verify(repository).search("Ashley");
    }

    @Test
    void searchSupportsStatusFilterAndEmptyQuery() throws Exception {
        when(repository.search("", MatchStatus.AMBIGUOUS)).thenReturn(List.of(resident("R-2", "Sam", "Lee", MatchStatus.AMBIGUOUS)));

        mvc.perform(get("/api/residents/search").param("q", "").param("status", "AMBIGUOUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].matchStatus").value("AMBIGUOUS"));
        verify(repository).search("", MatchStatus.AMBIGUOUS);
    }

    @Test
    void searchByIdCityAndXmlRefUsesRepositoryQuery() throws Exception {
        when(repository.search("R-1")).thenReturn(List.of(resident("R-1", "Jane", "Doe", MatchStatus.REST_ONLY)));
        mvc.perform(get("/api/residents/search").param("q", "R-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalResults").value(1));
        verify(repository).search("R-1");
    }

    @Test
    void noResultsReturnsEmptyResponse() throws Exception {
        when(repository.search("missing")).thenReturn(List.of());
        mvc.perform(get("/api/residents/search").param("q", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void detailPreservesBothSourceRepresentations() throws Exception {
        UnifiedResident value = resident("R-1", "Jane", "Doe", MatchStatus.EXACT)
                .xmlRef("X-1").xmlName("DOE, Jane").xmlBorn("1980-01-01").xmlAddr("1 Main St")
                .xmlTown("Town").xmlBenefitCode("BEN-1").xmlReviewDue("2026-01-01");
        when(repository.findByRestId("R-1")).thenReturn(Optional.of(value));

        mvc.perform(get("/api/residents/R-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rest.id").value("R-1"))
                .andExpect(jsonPath("$.xml.ref").value("X-1"))
                .andExpect(jsonPath("$.sourceAvailability.rest").value(true))
                .andExpect(jsonPath("$.sourceAvailability.xml").value(true));
    }

            @Test
            void detailExposesEvidenceAndConfidence() throws Exception {
            UnifiedResident value = resident("R-2", "Jane", "Doe", MatchStatus.PROBABLE)
                .matchConfidence(70).evidenceNameEqual(true).evidenceDobEqual(null)
                .evidenceAddressEqual(true).evidenceXmlDobMissing(true)
                .evidenceCandidateRefs("X-2").xmlRef("X-2").xmlName("DOE, Jane");
            when(repository.findByRestId("R-2")).thenReturn(Optional.of(value));

            mvc.perform(get("/api/residents/R-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchConfidence").value(70))
                .andExpect(jsonPath("$.evidence[0].comparison").value("MATCH"))
                .andExpect(jsonPath("$.evidence[1].comparison").value("MISSING"))
                .andExpect(jsonPath("$.evidence[0].candidateRefs[0]").value("X-2"));
            }

            @Test
            void restOnlyAndXmlOnlyDetailsDoNotFabricateSourceValues() throws Exception {
            UnifiedResident restOnly = resident("R-3", "Rest", "Only", MatchStatus.REST_ONLY);
            UnifiedResident xmlOnly = new UnifiedResident().xmlRef("X-3").xmlName("ONLY, Xml")
                .matchStatus(MatchStatus.XML_ONLY).matchConfidence(0).matchNotes("note");
            when(repository.findByRestId("R-3")).thenReturn(Optional.of(restOnly));
            when(repository.findByRestId("X-3")).thenReturn(Optional.empty());
            when(repository.findByXmlRef("X-3")).thenReturn(Optional.of(xmlOnly));

            mvc.perform(get("/api/residents/R-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rest.id").value("R-3"))
                .andExpect(jsonPath("$.xml").doesNotExist());
            mvc.perform(get("/api/residents/X-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rest").doesNotExist())
                .andExpect(jsonPath("$.xml.ref").value("X-3"));
            }

    @Test
    void missingDetailReturns404() throws Exception {
        when(repository.findByRestId("missing")).thenReturn(Optional.empty());
        when(repository.findByXmlRef("missing")).thenReturn(Optional.empty());
        mvc.perform(get("/api/residents/missing")).andExpect(status().isNotFound());
    }

    private static UnifiedResident resident(String id, String first, String last, MatchStatus status) {
        return new UnifiedResident().restId(id).restFirstName(first).restLastName(last)
                .restDateOfBirth("1980-01-01").restAddressLine("1 Main St").restCity("Town")
                .matchStatus(status).matchConfidence(status == MatchStatus.EXACT ? 100 : 0).matchNotes("note");
    }
}
