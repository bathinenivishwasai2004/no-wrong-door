package com.nowrongdoor.adapters.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper for the official XML Benefits Register response.
 * <p>
 * The service returns:
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <BenefitsRegister>
 *   <Record>
 *     <Ref>AS/2024/4702</Ref>
 *     <Name>EASTWOOD, Donna</Name>
 *     <Born>1973-11-18</Born>
 *     <Addr>137 Poplar Road</Addr>
 *     <Town>Ash Hill</Town>
 *     <BenefitCode>TRN-1</BenefitCode>
 *     <ReviewDue>2026-06-25</ReviewDue>
 *   </Record>
 *   ...
 * </BenefitsRegister>
 * }
 * </pre>
 */
@JacksonXmlRootElement(localName = "BenefitsRegister")
public final class BenefitsRegister {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Record")
    private List<XmlRecord> records = Collections.emptyList();

    public BenefitsRegister() {}

    public List<XmlRecord> getRecords() {
        return records != null ? records : Collections.emptyList();
    }

    public void setRecords(List<XmlRecord> records) {
        this.records = records;
    }

    @Override
    public String toString() {
        return "BenefitsRegister{records=" + (records != null ? records.size() : 0) + "}";
    }
}
