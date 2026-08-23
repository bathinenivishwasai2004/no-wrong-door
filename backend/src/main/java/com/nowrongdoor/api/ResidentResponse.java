package com.nowrongdoor.api;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;

import java.util.Arrays;
import java.util.List;

/** Public representation of a unified resident, without persistence details. */
public record ResidentResponse(
        Long id,
        MatchStatus matchStatus,
        int matchConfidence,
        String matchNotes,
        String name,
        String dateOfBirth,
        String address,
        String city,
        SourceAvailability sourceAvailability,
        RestSource rest,
        XmlSource xml,
        List<Evidence> evidence
) {
    public static ResidentResponse from(UnifiedResident resident) {
        boolean hasRest = resident.getRestId() != null;
        boolean hasXml = resident.getXmlRef() != null;
        return new ResidentResponse(
                resident.getId(), resident.getMatchStatus(), resident.getMatchConfidence(), resident.getMatchNotes(),
                resident.displayName(), hasRest ? resident.getRestDateOfBirth() : resident.getXmlBorn(),
                hasRest ? resident.getRestAddressLine() : resident.getXmlAddr(),
                hasRest ? resident.getRestCity() : resident.getXmlTown(),
                new SourceAvailability(hasRest, hasXml),
                hasRest ? new RestSource(resident.getRestId(), resident.getRestFirstName(), resident.getRestLastName(),
                        resident.getRestDateOfBirth(), resident.getRestAddressLine(), resident.getRestCity(),
                        resident.getRestPhone(), resident.getRestProgramStatus(), resident.getRestLastContact()) : null,
                hasXml ? new XmlSource(resident.getXmlRef(), resident.getXmlName(), resident.getXmlBorn(),
                        resident.getXmlAddr(), resident.getXmlTown(), resident.getXmlBenefitCode(), resident.getXmlReviewDue()) : null,
                evidenceFrom(resident));
    }

    private static List<Evidence> evidenceFrom(UnifiedResident resident) {
        List<String> refs = resident.getEvidenceCandidateRefs() == null || resident.getEvidenceCandidateRefs().isBlank()
                ? List.of() : Arrays.stream(resident.getEvidenceCandidateRefs().split(",")).filter(value -> !value.isBlank()).toList();
        boolean hasRest = resident.getRestId() != null;
        boolean hasXml = resident.getXmlRef() != null;
        return List.of(
                new Evidence("name", hasRest && hasXml ? (resident.isEvidenceNameEqual() ? "MATCH" : "DIFFERENT") : "NOT AVAILABLE",
                        hasRest && hasXml ? (resident.isEvidenceNameEqual() ? "Same normalized name" : "Names did not match") : "One source is unavailable", refs),
                new Evidence("date of birth", !hasRest || !hasXml ? "NOT AVAILABLE" : resident.getEvidenceDobEqual() == null ? "MISSING" : resident.getEvidenceDobEqual() ? "MATCH" : "DIFFERENT",
                        !hasRest || !hasXml ? "One source is unavailable" : resident.getEvidenceDobEqual() == null ? "A date of birth is unknown on one side" : resident.getEvidenceDobEqual() ? "Same date of birth" : "Conflicting dates of birth", refs),
                new Evidence("address and town", !hasRest || !hasXml ? "NOT AVAILABLE" : resident.isEvidenceAddressEqual() ? "NORMALIZED MATCH" : "DIFFERENT",
                        !hasRest || !hasXml ? "One source is unavailable" : resident.isEvidenceAddressEqual() ? "Address and place match after normalization" : "Address or place differs", refs),
                new Evidence("candidate resolution", resident.getMatchStatus() == MatchStatus.AMBIGUOUS ? "MANUAL REVIEW" : "STATUS",
                        resident.getMatchNotes(), refs));
    }

    public record SourceAvailability(boolean rest, boolean xml) {}

    public record RestSource(String id, String firstName, String lastName, String dateOfBirth,
                             String address, String city, String phone, String programStatus, String lastContact) {}

    public record XmlSource(String ref, String name, String born, String address,
                            String town, String benefitCode, String reviewDue) {}

        public record Evidence(String field, String comparison, String reason, List<String> candidateRefs) {}
}
