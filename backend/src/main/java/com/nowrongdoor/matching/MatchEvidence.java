package com.nowrongdoor.matching;

import java.util.List;

/**
 * Explainable comparison facts for one match decision.
 * Does not include phone, benefits, program status, last contact, or {@code _pid}.
 */
public record MatchEvidence(
        boolean nameEqual,
        Boolean dobEqual,
        boolean addressEqual,
        boolean xmlDobMissing,
        boolean restDobMissing,
        List<String> xmlCandidateRefs
) {
    public MatchEvidence {
        xmlCandidateRefs = xmlCandidateRefs == null ? List.of() : List.copyOf(xmlCandidateRefs);
    }

    /** {@code dobEqual} is {@code null} when either date of birth is missing. */
    public static MatchEvidence of(boolean nameEqual,
                                   Boolean dobEqual,
                                   boolean addressEqual,
                                   boolean xmlDobMissing,
                                   boolean restDobMissing,
                                   List<String> xmlCandidateRefs) {
        return new MatchEvidence(nameEqual, dobEqual, addressEqual, xmlDobMissing,
                restDobMissing, xmlCandidateRefs);
    }
}
