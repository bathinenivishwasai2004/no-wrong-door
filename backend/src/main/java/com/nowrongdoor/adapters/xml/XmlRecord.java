package com.nowrongdoor.adapters.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Internal model for a single benefit record from the XML Benefits Register.
 * <p>
 * XML element names use PascalCase (e.g., {@code <Ref>}, {@code <BenefitCode>})
 * as returned by the source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class XmlRecord {

    @JacksonXmlProperty(localName = "Ref")
    private String ref;

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Born")
    private String born;

    @JacksonXmlProperty(localName = "Addr")
    private String addr;

    @JacksonXmlProperty(localName = "Town")
    private String town;

    @JacksonXmlProperty(localName = "BenefitCode")
    private String benefitCode;

    @JacksonXmlProperty(localName = "ReviewDue")
    private String reviewDue;

    public XmlRecord() {}

    public String getRef()         { return ref; }
    public String getName()        { return name; }
    public String getBorn()        { return born; }
    public String getAddr()        { return addr; }
    public String getTown()        { return town; }
    public String getBenefitCode() { return benefitCode; }
    public String getReviewDue()   { return reviewDue; }

    public void setRef(String v)         { this.ref = v; }
    public void setName(String v)        { this.name = v; }
    public void setBorn(String v)        { this.born = v; }
    public void setAddr(String v)        { this.addr = v; }
    public void setTown(String v)        { this.town = v; }
    public void setBenefitCode(String v) { this.benefitCode = v; }
    public void setReviewDue(String v)   { this.reviewDue = v; }

    @Override
    public String toString() {
        return "XmlRecord{ref='" + ref + "', name='" + name + "', born='" + born + "'}";
    }
}
