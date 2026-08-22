package com.nowrongdoor.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Unified resident record stored in the application database.
 * <p>
 * Fields prefixed with {@code xml} hold values sourced from the XML Benefits Register.
 * REST-prefixed fields hold values sourced from the REST Resident Index.
 * Where both sources agree the value is stored once; where they differ both are kept
 * so discrepancies are never silently merged.
 * <p>
 * Fields annotated with <em>[app-generated]</em> are produced by the application
 * and are not present in either upstream source.
 * <p>
 * REST source field mapping (official contract):
 * <ul>
 *   <li>{@code id}             → restId</li>
 *   <li>{@code first_name}     → restFirstName</li>
 *   <li>{@code last_name}      → restLastName</li>
 *   <li>{@code date_of_birth}  → restDateOfBirth</li>
 *   <li>{@code address_line}   → restAddressLine</li>
 *   <li>{@code city}           → restCity</li>
 *   <li>{@code phone}          → restPhone</li>
 *   <li>{@code program_status} → restProgramStatus</li>
 *   <li>{@code last_contact}   → restLastContact</li>
 * </ul>
 * XML source field mapping (official contract):
 * <ul>
 *   <li>{@code <Ref>}         → xmlRef</li>
 *   <li>{@code <Name>}        → xmlName</li>
 *   <li>{@code <Born>}        → xmlBorn</li>
 *   <li>{@code <Addr>}        → xmlAddr</li>
 *   <li>{@code <Town>}        → xmlTown</li>
 *   <li>{@code <BenefitCode>} → xmlBenefitCode</li>
 *   <li>{@code <ReviewDue>}   → xmlReviewDue</li>
 * </ul>
 */
@Entity
@Table(name = "unified_residents")
public class UnifiedResident {

    /** [app-generated] Internal surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── REST source fields ─────────────────────────────────────────────────

    /** The resident ID from the REST Resident Index (e.g. "R-10394"). Primary REST key. */
    @Column(name = "rest_id")
    private String restId;

    @Column(name = "rest_first_name")
    private String restFirstName;

    @Column(name = "rest_last_name")
    private String restLastName;

    @Column(name = "rest_date_of_birth")
    private String restDateOfBirth;

    @Column(name = "rest_address_line")
    private String restAddressLine;

    @Column(name = "rest_city")
    private String restCity;

    @Column(name = "rest_phone")
    private String restPhone;

    @Column(name = "rest_program_status")
    private String restProgramStatus;

    @Column(name = "rest_last_contact")
    private String restLastContact;

    // ── XML source fields ─────────────────────────────────────────────────

    /** The reference number from the XML Benefits Register (e.g. "AS/2024/4702"). */
    @Column(name = "xml_ref")
    private String xmlRef;

    @Column(name = "xml_name")
    private String xmlName;

    @Column(name = "xml_born")
    private String xmlBorn;

    @Column(name = "xml_addr")
    private String xmlAddr;

    @Column(name = "xml_town")
    private String xmlTown;

    @Column(name = "xml_benefit_code")
    private String xmlBenefitCode;

    @Column(name = "xml_review_due")
    private String xmlReviewDue;

    // ── Application-generated fields ──────────────────────────────────────

    /** [app-generated] How well this record was matched across the two sources. */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    private MatchStatus matchStatus;

    /**
     * [app-generated] Matching confidence score 0–100.
     * <ul>
     *   <li>100 — EXACT: id + name + born all agree</li>
     *   <li>70  — PROBABLE: id matches but name/addr differs</li>
     *   <li>60  — PROBABLE: name + born match (no shared id)</li>
     *   <li>30  — AMBIGUOUS: multiple XML candidates</li>
     *   <li>0   — REST_ONLY or XML_ONLY</li>
     * </ul>
     */
    @Column(name = "match_confidence")
    private int matchConfidence;

    /** [app-generated] Human-readable note explaining the match decision. */
    @Column(name = "match_notes", length = 500)
    private String matchNotes;

    /** [app-generated] When this record was written to the database. */
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    // ── Constructors ──────────────────────────────────────────────────────

    public UnifiedResident() {}

    // ── Builder-style setters (fluent) ────────────────────────────────────

    public UnifiedResident restId(String v)            { this.restId = v; return this; }
    public UnifiedResident restFirstName(String v)     { this.restFirstName = v; return this; }
    public UnifiedResident restLastName(String v)      { this.restLastName = v; return this; }
    public UnifiedResident restDateOfBirth(String v)   { this.restDateOfBirth = v; return this; }
    public UnifiedResident restAddressLine(String v)   { this.restAddressLine = v; return this; }
    public UnifiedResident restCity(String v)          { this.restCity = v; return this; }
    public UnifiedResident restPhone(String v)         { this.restPhone = v; return this; }
    public UnifiedResident restProgramStatus(String v) { this.restProgramStatus = v; return this; }
    public UnifiedResident restLastContact(String v)   { this.restLastContact = v; return this; }

    public UnifiedResident xmlRef(String v)            { this.xmlRef = v; return this; }
    public UnifiedResident xmlName(String v)           { this.xmlName = v; return this; }
    public UnifiedResident xmlBorn(String v)           { this.xmlBorn = v; return this; }
    public UnifiedResident xmlAddr(String v)           { this.xmlAddr = v; return this; }
    public UnifiedResident xmlTown(String v)           { this.xmlTown = v; return this; }
    public UnifiedResident xmlBenefitCode(String v)    { this.xmlBenefitCode = v; return this; }
    public UnifiedResident xmlReviewDue(String v)      { this.xmlReviewDue = v; return this; }

    public UnifiedResident matchStatus(MatchStatus v)  { this.matchStatus = v; return this; }
    public UnifiedResident matchConfidence(int v)      { this.matchConfidence = v; return this; }
    public UnifiedResident matchNotes(String v)        { this.matchNotes = v; return this; }
    public UnifiedResident ingestedAt(Instant v)       { this.ingestedAt = v; return this; }

    // ── Getters ───────────────────────────────────────────────────────────

    public Long getId()                { return id; }
    public String getRestId()          { return restId; }
    public String getRestFirstName()   { return restFirstName; }
    public String getRestLastName()    { return restLastName; }
    public String getRestDateOfBirth() { return restDateOfBirth; }
    public String getRestAddressLine() { return restAddressLine; }
    public String getRestCity()        { return restCity; }
    public String getRestPhone()       { return restPhone; }
    public String getRestProgramStatus(){ return restProgramStatus; }
    public String getRestLastContact() { return restLastContact; }

    public String getXmlRef()         { return xmlRef; }
    public String getXmlName()        { return xmlName; }
    public String getXmlBorn()        { return xmlBorn; }
    public String getXmlAddr()        { return xmlAddr; }
    public String getXmlTown()        { return xmlTown; }
    public String getXmlBenefitCode() { return xmlBenefitCode; }
    public String getXmlReviewDue()   { return xmlReviewDue; }

    public MatchStatus getMatchStatus()  { return matchStatus; }
    public int getMatchConfidence()      { return matchConfidence; }
    public String getMatchNotes()        { return matchNotes; }
    public Instant getIngestedAt()       { return ingestedAt; }

    /** Returns the best available display name (REST first name + last name, then XML full name). */
    public String displayName() {
        if (restFirstName != null || restLastName != null) {
            return ((restFirstName != null ? restFirstName : "") + " "
                    + (restLastName != null ? restLastName : "")).strip();
        }
        return xmlName;
    }

    /** Returns the best available identifier (REST id first, then XML ref). */
    public String displayRef() {
        return restId != null ? restId : xmlRef;
    }
}
