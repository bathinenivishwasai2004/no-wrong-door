package com.nowrongdoor.repository;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class UnifiedResidentRepositorySearchTest {
    @Autowired private UnifiedResidentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(resident("R-1", "Maria", "Lindquist", "Springfield", "12 Main Street", MatchStatus.EXACT));
        repository.save(resident("R-2", "John", "Smith", "Shelbyville", "99 Side Street", MatchStatus.AMBIGUOUS));
    }

    @Test
    void searchesRestFullNameCaseInsensitivelyAndPreservesPartialNames() {
        assertResident("Maria", MatchStatus.EXACT);
        assertResident("Lindquist", MatchStatus.EXACT);
        assertResident("Maria Lindquist", MatchStatus.EXACT);
        assertResident("maria lindquist", MatchStatus.EXACT);
        assertResident("MARIA LINDQUIST", MatchStatus.EXACT);
    }

    @Test
    void searchesExistingFieldsAndAppliesStatusFilter() {
        assertResident("R-1", MatchStatus.EXACT);
        assertResident("Springfield", MatchStatus.EXACT);
        assertResident("12 Main", MatchStatus.EXACT);
        assertEquals(List.of("R-2"), repository.search("John", MatchStatus.AMBIGUOUS)
                .stream().map(UnifiedResident::getRestId).toList());
    }

    private void assertResident(String query, MatchStatus status) {
        assertEquals(List.of("R-1"), repository.search(query).stream()
                .map(UnifiedResident::getRestId).toList());
        assertEquals(List.of("R-1"), repository.search(query, status).stream()
                .map(UnifiedResident::getRestId).toList());
    }

    private static UnifiedResident resident(String id, String firstName, String lastName,
                                            String city, String address, MatchStatus status) {
        return new UnifiedResident().restId(id).restFirstName(firstName).restLastName(lastName)
            .restCity(city).restAddressLine(address).matchStatus(status).ingestedAt(Instant.EPOCH);
    }
}