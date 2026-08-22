package com.nowrongdoor.model;

/**
 * Classification of how well a unified resident record was matched
 * across the REST and XML sources.
 *
 * <ul>
 *   <li>{@link #EXACT}     — same {@code ref} confirmed by matching name and date of birth</li>
 *   <li>{@link #PROBABLE}  — same {@code ref} but name or address differs slightly,
 *                            OR no shared {@code ref} but name + born match</li>
 *   <li>{@link #AMBIGUOUS} — multiple XML candidates match the REST resident
 *                            (cannot safely merge without human review)</li>
 *   <li>{@link #REST_ONLY} — present in the REST source only; no XML counterpart found</li>
 *   <li>{@link #XML_ONLY}  — present in the XML source only; no REST counterpart found</li>
 * </ul>
 */
public enum MatchStatus {
    EXACT,
    PROBABLE,
    AMBIGUOUS,
    REST_ONLY,
    XML_ONLY
}
