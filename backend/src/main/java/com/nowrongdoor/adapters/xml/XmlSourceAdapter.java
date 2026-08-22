package com.nowrongdoor.adapters.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;


@Component
public class XmlSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(XmlSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final XmlSourceConfig config;
    private final XmlMapper xmlMapper;

    public XmlSourceAdapter(RestTemplate restTemplate,
                            XmlSourceConfig config,
                            @org.springframework.beans.factory.annotation.Qualifier("xmlMapper") XmlMapper xmlMapper) {
        this.restTemplate = restTemplate;
        this.config       = config;
        this.xmlMapper    = xmlMapper;
    }

    /**
     * Fetch all benefit records from the XML Benefits Register.
     * Retries with exponential backoff on 5xx or network errors.
     *
     * @return {@link XmlFetchResult.Success} with parsed records,
     *         or {@link XmlFetchResult.Failure} if all attempts fail
     */
    public XmlFetchResult fetchAllRecords() {
        String url = config.getBaseUrl() + "/records";
        int maxRetries      = config.getMaxRetries();
        long backoffMs      = config.getInitialBackoffMs();
        int attempt         = 0;
        Exception lastCause = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                log.info("XML: attempt {}/{} — GET {}", attempt, maxRetries, url);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is5xxServerError()) {
                    log.warn("XML: attempt {} got HTTP {} — will retry", attempt, response.getStatusCode());
                    sleep(backoffMs);
                    backoffMs *= 2;
                    continue;
                }

                if (!response.getStatusCode().is2xxSuccessful()) {
                    return new XmlFetchResult.Failure(
                            "XML source returned unexpected status: " + response.getStatusCode(), attempt);
                }

                String body = response.getBody();
                if (body == null || body.isBlank()) {
                    return new XmlFetchResult.Failure("XML source returned empty body", attempt);
                }

                List<XmlRecord> records = parseXml(body);
                log.info("XML: success on attempt {} — {} records parsed", attempt, records.size());
                return new XmlFetchResult.Success(records, attempt);

            } catch (HttpServerErrorException e) {
                lastCause = e;
                log.warn("XML: attempt {} got server error: {} — will retry", attempt, e.getStatusCode());
                sleep(backoffMs);
                backoffMs *= 2;

            } catch (ResourceAccessException e) {
                lastCause = e;
                log.warn("XML: attempt {} timeout/connection error: {} — will retry", attempt, e.getMessage());
                sleep(backoffMs);
                backoffMs *= 2;

            } catch (RestClientException e) {
                lastCause = e;
                log.warn("XML: attempt {} client error (not retryable): {}", attempt, e.getMessage());
                return new XmlFetchResult.Failure("XML client error: " + e.getMessage(), attempt, e);

            } catch (Exception e) {
                log.error("XML: unexpected error on attempt {}", attempt, e);
                return new XmlFetchResult.Failure("Unexpected error: " + e.getMessage(), attempt, e);
            }
        }

        log.error("XML: all {} attempts failed", maxRetries);
        return new XmlFetchResult.Failure(
                "XML source failed after " + maxRetries + " attempts", attempt, lastCause);
    }

    /**
     * Parse XML body into a list of {@link XmlRecord}.
     * Handles the official {@code <BenefitsRegister><Record>...</Record></BenefitsRegister>} envelope.
     */
    private List<XmlRecord> parseXml(String body) throws Exception {
        // Try to deserialize as a wrapper; fall back to empty list on parse failure
        try {
            BenefitsRegister wrapper = xmlMapper.readValue(body, BenefitsRegister.class);
            return wrapper.getRecords() != null ? wrapper.getRecords() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("XML: failed to parse response as BenefitsRegister: {}", e.getMessage());
            throw e;
        }
    }

    /** Health check — /health has no delay or failure simulation. */
    public boolean checkHealth() {
        try {
            ResponseEntity<String> r = restTemplate.getForEntity(
                    config.getBaseUrl() + "/health", String.class);
            return r.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("XML health check failed: {}", e.getMessage());
            return false;
        }
    }

    private void sleep(long ms) {
        try {
            log.debug("XML: backing off for {}ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
