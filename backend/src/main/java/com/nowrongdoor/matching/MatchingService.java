package com.nowrongdoor.matching;

import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.matching.normalize.AddressNormalizer;
import com.nowrongdoor.matching.normalize.DateNormalizer;
import com.nowrongdoor.matching.normalize.NameNormalizer;
import com.nowrongdoor.model.MatchStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST-driven entity resolution between the Resident Index and the Benefits Register.
 * <p>
 * Pure business logic: no repositories, no HTTP, no {@code _pid}.
 * Identity evidence is limited to normalized name, date of birth, and address+place.
 */
@Service
public class MatchingService {

    public static final int CONFIDENCE_EXACT = 100;
    public static final int CONFIDENCE_PROBABLE = 70;
    public static final int CONFIDENCE_AMBIGUOUS = 30;
    public static final int CONFIDENCE_UNMATCHED = 0;

    private final NameNormalizer names;
    private final AddressNormalizer addresses;
    private final DateNormalizer dates;

    public MatchingService() {
        this(new NameNormalizer(), new AddressNormalizer(), new DateNormalizer());
    }

    public MatchingService(NameNormalizer names,
                           AddressNormalizer addresses,
                           DateNormalizer dates) {
        this.names = names;
        this.addresses = addresses;
        this.dates = dates;
    }

    public List<MatchResult> match(List<RestResident> restRecords, List<XmlRecord> xmlRecords) {
        List<RestResident> rest = restRecords == null ? List.of() : restRecords;
        List<XmlRecord> xml = xmlRecords == null ? List.of() : xmlRecords;

        XmlIndexes indexes = buildIndexes(xml);
        Set<String> usedXmlRefs = new HashSet<>();
        List<MatchResult> results = new ArrayList<>();

        for (RestResident resident : rest) {
            results.add(matchOne(resident, indexes, usedXmlRefs));
        }

        xml.stream()
                .sorted(Comparator.comparing(r -> nullToEmpty(r.getRef())))
                .filter(r -> r.getRef() != null && !usedXmlRefs.contains(r.getRef()))
                .forEach(record -> results.add(xmlOnly(record)));

        return results;
    }

    private MatchResult matchOne(RestResident rest,
                                 XmlIndexes indexes,
                                 Set<String> usedXmlRefs) {
        Optional<NameKey> restName = names.fromRest(rest.getFirstName(), rest.getLastName());
        if (restName.isEmpty()) {
            return restOnly(rest, false, dates.isMissing(rest.getDateOfBirth()),
                    false, dates.isMissing(rest.getDateOfBirth()), List.of());
        }

        List<XmlRecord> nameCandidates = unused(indexes.byName.getOrDefault(restName.get(), List.of()),
                usedXmlRefs);

        if (nameCandidates.isEmpty()) {
            return restOnly(rest, false, dates.isMissing(rest.getDateOfBirth()),
                    false, dates.isMissing(rest.getDateOfBirth()), List.of());
        }

        Optional<String> restDob = dates.canonicalize(rest.getDateOfBirth());
        List<XmlRecord> exactDob = new ArrayList<>();
        List<XmlRecord> missingXmlDob = new ArrayList<>();
        for (XmlRecord candidate : nameCandidates) {
            Optional<String> xmlDob = dates.canonicalize(candidate.getBorn());
            if (restDob.isPresent() && xmlDob.isPresent() && restDob.get().equals(xmlDob.get())) {
                exactDob.add(candidate);
            } else if (xmlDob.isEmpty()) {
                missingXmlDob.add(candidate);
            }
            // both present and conflicting → homonym, never a match for this REST row
        }

        List<String> nameRefs = refs(nameCandidates);

        if (exactDob.size() > 1) {
            return ambiguous(rest, true, true, false,
                    dates.isMissing(rest.getDateOfBirth()), refs(exactDob),
                    "name equal; dob equal " + restDob.get()
                            + "; multiple XML candidates share name + DOB; refs="
                            + refs(exactDob));
        }
        if (exactDob.size() == 1) {
            XmlRecord xml = exactDob.get(0);
            boolean addrEqual = addressEqual(rest, xml);
            usedXmlRefs.add(xml.getRef());
            String notes = exactNotes(rest, xml, restDob.get(), addrEqual);
            return new MatchResult(rest, xml, MatchStatus.EXACT, CONFIDENCE_EXACT, notes,
                    MatchEvidence.of(true, true, addrEqual, false, false, List.of(xml.getRef())));
        }

        // No unique name+DOB pair. Consider missing XML DOB among remaining unused name matches.
        if (missingXmlDob.isEmpty()) {
            return restOnly(rest, true, dates.isMissing(rest.getDateOfBirth()),
                    false, dates.isMissing(rest.getDateOfBirth()), nameRefs);
        }

        List<XmlRecord> addressHits = missingXmlDob.stream()
                .filter(x -> addressEqual(rest, x))
                .toList();

        if (addressHits.size() == 1) {
            XmlRecord xml = addressHits.get(0);
            usedXmlRefs.add(xml.getRef());
            String suffixNote = suffixEvidence(rest, xml);
                String notes = "Name matches; XML DOB missing; address matches after normalization."
                    + (suffixNote.isEmpty() ? "" : " (" + suffixNote + ").");
            return new MatchResult(rest, xml, MatchStatus.PROBABLE, CONFIDENCE_PROBABLE, notes,
                    MatchEvidence.of(true, null, true, true,
                            dates.isMissing(rest.getDateOfBirth()), List.of(xml.getRef())));
        }

        if (missingXmlDob.size() == 1 && addressHits.isEmpty()) {
            XmlRecord xml = missingXmlDob.get(0);
            String notes = "name equal; XML DOB missing; address conflicts; unsafe candidate; ref="
                    + nullToEmpty(xml.getRef());
            return ambiguous(rest, true, null, false, dates.isMissing(rest.getDateOfBirth()),
                    List.of(xml.getRef()), notes);
        }

        String notes = "Multiple plausible candidates; manual review required. refs="
                + refs(missingXmlDob);
        return ambiguous(rest, true, null, false, dates.isMissing(rest.getDateOfBirth()),
                refs(missingXmlDob), notes);
    }

    private String exactNotes(RestResident rest, XmlRecord xml, String dob, boolean addrEqual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name and DOB match exactly.");
        if (addrEqual) {
            String suffix = suffixEvidence(rest, xml);
            sb.append(" Address matches after normalization.");
            if (!suffix.isEmpty()) {
                sb.append(" (").append(suffix).append(").");
            }
        }
        return sb.toString();
    }

    private String suffixEvidence(RestResident rest, XmlRecord xml) {
        String restSuffix = addresses.expandedSuffixLabel(rest.getAddressLine());
        if (!restSuffix.isEmpty()) {
            return restSuffix;
        }
        return addresses.expandedSuffixLabel(xml.getAddr());
    }

    private boolean addressEqual(RestResident rest, XmlRecord xml) {
        Optional<AddressAndPlaceKey> restKey = addresses.key(rest.getAddressLine(), rest.getCity());
        Optional<AddressAndPlaceKey> xmlKey = addresses.key(xml.getAddr(), xml.getTown());
        return restKey.isPresent() && xmlKey.isPresent() && restKey.get().equals(xmlKey.get());
    }

    private MatchResult restOnly(RestResident rest,
                                 boolean nameEqual,
                                 boolean restDobMissing,
                                 boolean addressEqual,
                                 boolean xmlDobMissing,
                                 List<String> candidateRefs) {
        return new MatchResult(rest, null, MatchStatus.REST_ONLY, CONFIDENCE_UNMATCHED,
                "No sufficiently credible XML match.",
                MatchEvidence.of(nameEqual, restDobMissing ? null : Boolean.FALSE,
                        addressEqual, xmlDobMissing, restDobMissing, candidateRefs));
    }

    private MatchResult xmlOnly(XmlRecord xml) {
        return new MatchResult(null, xml, MatchStatus.XML_ONLY, CONFIDENCE_UNMATCHED,
                "No sufficiently credible REST match.",
                MatchEvidence.of(false, null, false, dates.isMissing(xml.getBorn()), true,
                        xml.getRef() == null ? List.of() : List.of(xml.getRef())));
    }

    private MatchResult ambiguous(RestResident rest,
                                  boolean nameEqual,
                                  Boolean dobEqual,
                                  boolean addressEqual,
                                  boolean restDobMissing,
                                  List<String> candidateRefs,
                                  String notes) {
        return new MatchResult(rest, null, MatchStatus.AMBIGUOUS, CONFIDENCE_AMBIGUOUS, notes,
                MatchEvidence.of(nameEqual, dobEqual, addressEqual,
                        dobEqual == null, restDobMissing, candidateRefs));
    }

    private static List<XmlRecord> unused(List<XmlRecord> candidates, Set<String> usedXmlRefs) {
        return candidates.stream()
                .filter(x -> x.getRef() != null && !usedXmlRefs.contains(x.getRef()))
                .toList();
    }

    private static List<String> refs(List<XmlRecord> records) {
        return records.stream()
                .map(XmlRecord::getRef)
                .map(MatchingService::nullToEmpty)
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    XmlIndexes buildIndexes(List<XmlRecord> xml) {
        Map<NameKey, List<XmlRecord>> byName = new HashMap<>();
        Map<NameAndDobKey, List<XmlRecord>> byNameAndDob = new HashMap<>();
        Map<AddressAndPlaceKey, List<XmlRecord>> byAddress = new HashMap<>();

        for (XmlRecord record : xml) {
            names.fromXml(record.getName()).ifPresent(name -> {
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(record);
                dates.canonicalize(record.getBorn()).ifPresent(dob ->
                        byNameAndDob.computeIfAbsent(new NameAndDobKey(name, dob), k -> new ArrayList<>())
                                .add(record));
            });
            addresses.key(record.getAddr(), record.getTown()).ifPresent(key ->
                    byAddress.computeIfAbsent(key, k -> new ArrayList<>()).add(record));
        }

        byName.values().forEach(list -> list.sort(Comparator.comparing(r -> nullToEmpty(r.getRef()))));
        byNameAndDob.values().forEach(list -> list.sort(Comparator.comparing(r -> nullToEmpty(r.getRef()))));
        byAddress.values().forEach(list -> list.sort(Comparator.comparing(r -> nullToEmpty(r.getRef()))));

        return new XmlIndexes(Map.copyOf(byName), Map.copyOf(byNameAndDob), Map.copyOf(byAddress));
    }

    record XmlIndexes(
            Map<NameKey, List<XmlRecord>> byName,
            Map<NameAndDobKey, List<XmlRecord>> byNameAndDob,
            Map<AddressAndPlaceKey, List<XmlRecord>> byAddress
    ) {
        XmlIndexes {
            byName = byName == null ? Map.of() : byName;
            byNameAndDob = byNameAndDob == null ? Map.of() : byNameAndDob;
            byAddress = byAddress == null ? Map.of() : byAddress;
        }
    }
}
