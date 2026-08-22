package com.nowrongdoor.matching;

import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.model.MatchStatus;

/**
 * One matching decision. {@code rest} is null for {@link MatchStatus#XML_ONLY}.
 * {@code xml} is assigned only when the pair is actually merged (EXACT or PROBABLE).
 */
public record MatchResult(
        RestResident rest,
        XmlRecord xml,
        MatchStatus status,
        int matchConfidence,
        String matchNotes,
        MatchEvidence evidence
) {}
