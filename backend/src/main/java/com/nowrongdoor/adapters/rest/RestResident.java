package com.nowrongdoor.adapters.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Internal model for a single resident record from the REST Resident Index.
 * <p>
 * Field names map directly from the official JSON response of
 * {@code GET /residents?page=1&page_size=25}.
 *
 * <pre>
 * {
 *   "id":             "R-10394",
 *   "first_name":     "Paul",
 *   "last_name":      "Quill",
 *   "date_of_birth":  "1955-06-10",
 *   "address_line":   "261 Sycamore Dr",
 *   "city":           "Weybridge",
 *   "phone":          "555-375-2897",
 *   "program_status": "Suspended",
 *   "last_contact":   "2025-04-07"
 * }
 * </pre>
 *
 * Equality and hashing are defined on {@code id}, which is the official
 * deduplication key for the paginated REST source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestResident {

    private String id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("address_line")
    private String addressLine;

    private String city;

    private String phone;

    @JsonProperty("program_status")
    private String programStatus;

    @JsonProperty("last_contact")
    private String lastContact;

    public RestResident() {}

    // ── Accessors ──────────────────────────────────────────────────────────

    public String getId()            { return id; }
    public String getFirstName()     { return firstName; }
    public String getLastName()      { return lastName; }
    public String getDateOfBirth()   { return dateOfBirth; }
    public String getAddressLine()   { return addressLine; }
    public String getCity()          { return city; }
    public String getPhone()         { return phone; }
    public String getProgramStatus() { return programStatus; }
    public String getLastContact()   { return lastContact; }

    // Setters required by Jackson
    public void setId(String v)            { this.id = v; }
    public void setFirstName(String v)     { this.firstName = v; }
    public void setLastName(String v)      { this.lastName = v; }
    public void setDateOfBirth(String v)   { this.dateOfBirth = v; }
    public void setAddressLine(String v)   { this.addressLine = v; }
    public void setCity(String v)          { this.city = v; }
    public void setPhone(String v)         { this.phone = v; }
    public void setProgramStatus(String v) { this.programStatus = v; }
    public void setLastContact(String v)   { this.lastContact = v; }

    /**
     * Equality on {@code id}. Two residents with the same source ID
     * are the same person regardless of which page they appeared on.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestResident that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "RestResident{id='" + id + "', firstName='" + firstName
               + "', lastName='" + lastName + "', dob='" + dateOfBirth + "'}";
    }
}
