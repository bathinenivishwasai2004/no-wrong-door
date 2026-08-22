package com.nowrongdoor.matching;

import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.matching.normalize.AddressNormalizer;
import com.nowrongdoor.matching.normalize.DateNormalizer;
import com.nowrongdoor.matching.normalize.NameNormalizer;
import com.nowrongdoor.model.MatchStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {
    private final NameNormalizer names = new NameNormalizer();
    private final AddressNormalizer addresses = new AddressNormalizer();
    private final DateNormalizer dates = new DateNormalizer();
    private final MatchingService service = new MatchingService(names, addresses, dates);

    @Test
    void normalizersHandleCasePunctuationXmlNamesAndDates() {
        assertEquals(new NameKey("mary ann", "o neil"), names.fromRest(" Mary-Ann ", "O'Neil").orElseThrow());
        assertEquals(new NameKey("mary ann", "o neil"), names.fromXml(" O'NEIL, Mary-Ann ").orElseThrow());
        assertEquals("1980-02-03", dates.canonicalize(" 1980-02-03 ").orElseThrow());
        assertTrue(dates.canonicalize("1980-02-30").isEmpty());
    }

    @Test
    void allAddressSuffixesNormalize() {
        assertEquals("1 oak street", addresses.normalizeLine("1 Oak St"));
        assertEquals("2 elm road", addresses.normalizeLine("2 Elm Rd"));
        assertEquals("3 pine avenue", addresses.normalizeLine("3 Pine Ave"));
        assertEquals("4 cedar drive", addresses.normalizeLine("4 Cedar Dr"));
        assertEquals("5 birch lane", addresses.normalizeLine("5 Birch Ln"));
    }

    @Test
    void exactMatchUsesNameAndDobAndNeverPid() {
        RestResident rest = rest("R-1", "Alice", "Smith", "1980-01-02", "1 Oak St", "Town");
        XmlRecord xml = xml("X-1", "SMITH, Alice", "1980-01-02", "1 Oak Street", "Town");

        MatchResult result = service.match(List.of(rest), List.of(xml)).get(0);

        assertEquals(MatchStatus.EXACT, result.status());
        assertEquals(100, result.matchConfidence());
        assertSame(xml, result.xml());
        assertTrue(result.matchNotes().contains("Name and DOB match exactly"));
        assertFalse(result.matchNotes().contains("_pid"));
    }

    @Test
    void probableRequiresMissingXmlDobAndMatchingAddressAndPlace() {
        MatchResult result = service.match(
                List.of(rest("R-1", "Jane", "Doe", "1975-03-04", "2 Elm Rd", "Riverside")),
                List.of(xml("X-1", "DOE, Jane", null, "2 Elm Road", "Riverside"))).get(0);

        assertEquals(MatchStatus.PROBABLE, result.status());
        assertEquals(70, result.matchConfidence());
        assertTrue(result.matchNotes().contains("XML DOB missing"));
    }

    @Test
    void missingDobWithConflictingAddressIsAmbiguous() {
        MatchResult result = service.match(
                List.of(rest("R-1", "Jane", "Doe", "1975-03-04", "2 Elm Rd", "Riverside")),
                List.of(xml("X-1", "DOE, Jane", null, "9 Oak St", "Riverside"))).get(0);

        assertEquals(MatchStatus.AMBIGUOUS, result.status());
        assertEquals(30, result.matchConfidence());
        assertNull(result.xml());
    }

    @Test
    void conflictingDobIsNeverMerged() {
        MatchResult result = service.match(
                List.of(rest("R-1", "John", "Smith", "1980-01-02", "1 Oak St", "Town")),
                List.of(xml("X-1", "SMITH, John", "1981-01-02", "1 Oak St", "Town"))).get(0);

        assertEquals(MatchStatus.REST_ONLY, result.status());
        assertNull(result.xml());
    }

    @Test
    void duplicateNameWithOneMatchingDobSelectsMatchingCandidate() {
        RestResident rest = rest("R-1", "John", "Smith", "1980-01-02", "1 Oak St", "Town");
        XmlRecord wrong = xml("X-1", "SMITH, John", "1981-01-02", "9 Oak St", "Town");
        XmlRecord right = xml("X-2", "SMITH, John", "1980-01-02", "1 Oak St", "Town");

        MatchResult result = service.match(List.of(rest), List.of(wrong, right)).get(0);

        assertEquals(MatchStatus.EXACT, result.status());
        assertSame(right, result.xml());
    }

    @Test
    void multipleMissingDobCandidatesAreAmbiguous() {
        MatchResult result = service.match(
                List.of(rest("R-1", "Sam", "Lee", "1990-01-01", "1 Oak St", "Town")),
                List.of(xml("X-1", "LEE, Sam", null, "1 Oak St", "Town"),
                        xml("X-2", "LEE, Sam", null, "1 Oak St", "Town"))).get(0);

        assertEquals(MatchStatus.AMBIGUOUS, result.status());
        assertTrue(result.matchNotes().contains("manual review required"));
    }

    @Test
    void missingAddressCannotCreateMatch() {
        MatchResult result = service.match(
                List.of(rest("R-1", "Jane", "Doe", "1975-03-04", null, "Riverside")),
                List.of(xml("X-1", "DOE, Jane", null, "2 Elm Road", "Riverside"))).get(0);

        assertEquals(MatchStatus.AMBIGUOUS, result.status());
    }

    @Test
    void restOnlyAndXmlOnlyHaveZeroConfidence() {
        List<MatchResult> results = service.match(
                List.of(rest("R-1", "Unique", "Person", "1975-03-04", "2 Elm Rd", "Riverside")),
                List.of(xml("X-1", "DOE, Jane", "1975-03-04", "2 Elm Rd", "Riverside")));

        assertEquals(List.of(MatchStatus.REST_ONLY, MatchStatus.XML_ONLY),
                results.stream().map(MatchResult::status).toList());
        assertTrue(results.stream().allMatch(result -> result.matchConfidence() == 0));
    }

    @Test
    void oneXmlRecordCannotBeAssignedTwice() {
        XmlRecord xml = xml("X-1", "SMITH, John", "1980-01-02", "1 Oak St", "Town");
        List<MatchResult> results = service.match(
                List.of(rest("R-1", "John", "Smith", "1980-01-02", "1 Oak St", "Town"),
                        rest("R-2", "John", "Smith", "1980-01-02", "1 Oak St", "Town")),
                List.of(xml));

        assertEquals(1, results.stream().filter(result -> result.xml() == xml).count());
        assertEquals(1, results.stream().filter(result -> result.status() == MatchStatus.EXACT).count());
    }

    @Test
    void outputIsDeterministic() {
        List<RestResident> rest = List.of(rest("R-1", "Alice", "Smith", "1980-01-02", "1 Oak St", "Town"));
        List<XmlRecord> xml = List.of(xml("X-2", "DOE, Jane", "1975-03-04", "2 Elm Rd", "Town"),
                xml("X-1", "SMITH, Alice", "1980-01-02", "1 Oak St", "Town"));

        assertEquals(service.match(rest, xml), service.match(rest, xml));
    }

    private static RestResident rest(String id, String first, String last, String dob, String address, String city) {
        RestResident resident = new RestResident();
        resident.setId(id); resident.setFirstName(first); resident.setLastName(last);
        resident.setDateOfBirth(dob); resident.setAddressLine(address); resident.setCity(city);
        return resident;
    }

    private static XmlRecord xml(String ref, String name, String born, String address, String town) {
        XmlRecord record = new XmlRecord();
        record.setRef(ref); record.setName(name); record.setBorn(born);
        record.setAddr(address); record.setTown(town);
        return record;
    }
}
