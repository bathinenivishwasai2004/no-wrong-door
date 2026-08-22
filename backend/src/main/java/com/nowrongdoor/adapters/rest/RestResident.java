package com.nowrongdoor.adapters.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Internal model for a single resident record from the REST source.
 * <p>
 * This class is intentionally kept in the {@code adapters.rest} package and
 * is not shared with the XML adapter or any aggregation layer. Phase 2 will
 * introduce a unified resident model that both adapters map into.
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures that if the REST
 * source adds new fields in future, deserialization does not break.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestResident {

    private String id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String address;
    private String phone;

    /** Required by Jackson for deserialization. */
    public RestResident() {}

    public RestResident(String id, String firstName, String lastName,
                        String dateOfBirth, String address, String phone) {
        this.id          = id;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address     = address;
        this.phone       = phone;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getFirstName()   { return firstName; }
    public String getLastName()    { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getAddress()     { return address; }
    public String getPhone()       { return phone; }

    // Setters required by Jackson
    public void setId(String id)                   { this.id = id; }
    public void setFirstName(String firstName)      { this.firstName = firstName; }
    public void setLastName(String lastName)        { this.lastName = lastName; }
    public void setDateOfBirth(String dateOfBirth)  { this.dateOfBirth = dateOfBirth; }
    public void setAddress(String address)          { this.address = address; }
    public void setPhone(String phone)              { this.phone = phone; }

    /**
     * Equality is based solely on {@code id}.
     * Two residents with the same source ID are the same person regardless
     * of which page they appeared on.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestResident that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RestResident{id='" + id + "', firstName='" + firstName +
               "', lastName='" + lastName + "'}";
    }
}
