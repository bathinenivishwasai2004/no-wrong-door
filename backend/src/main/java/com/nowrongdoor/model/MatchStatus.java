package com.nowrongdoor.model;

/**
 * Classification of how well a unified resident record was matched
 * across the REST and XML sources.
 * <p>
 * REST {@code id} and XML {@code Ref} are different namespaces and are never
 * compared. Matching uses normalized name, date of birth, and address+place only.
 *
 * <ul>
 *   <li>{@link #EXACT}     — unique unused XML candidate with the same normalized
 *                            given+surname and the same canonical DOB</li>
 *   <li>{@link #PROBABLE}  — XML DOB missing; unique unused name candidate (or
 *                            unique address disambiguation among missing-DOB
 *                            namesakes) with matching normalized address+place</li>
 *   <li>{@link #AMBIGUOUS} — multiple plausible XML candidates, or a single
 *                            missing-DOB namesake whose address conflicts</li>
 *   <li>{@link #REST_ONLY} — present in the REST source only; no credible XML pair</li>
 *   <li>{@link #XML_ONLY}  — unused XML record after REST-driven assignment</li>
 * </ul>
 */
public enum MatchStatus {
    EXACT,
    PROBABLE,
    AMBIGUOUS,
    REST_ONLY,
    XML_ONLY
}
