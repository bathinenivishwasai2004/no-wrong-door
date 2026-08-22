package com.nowrongdoor.api;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;

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
        XmlSource xml
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
                        resident.getXmlAddr(), resident.getXmlTown(), resident.getXmlBenefitCode(), resident.getXmlReviewDue()) : null);
    }

    public record SourceAvailability(boolean rest, boolean xml) {}

    public record RestSource(String id, String firstName, String lastName, String dateOfBirth,
                             String address, String city, String phone, String programStatus, String lastContact) {}

    public record XmlSource(String ref, String name, String born, String address,
                            String town, String benefitCode, String reviewDue) {}
}
