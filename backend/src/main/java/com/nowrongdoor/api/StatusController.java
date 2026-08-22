package com.nowrongdoor.api;

import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import com.nowrongdoor.adapters.xml.XmlSourceAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Status endpoints that report connectivity to each mock data source.
 * <p>
 * The frontend calls these to render source-status indicators.
 * Each endpoint independently checks its adapter — they do NOT
 * share state or cross-reference each other.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final RestSourceAdapter restSourceAdapter;
    private final XmlSourceAdapter xmlSourceAdapter;

    public StatusController(RestSourceAdapter restSourceAdapter,
                            XmlSourceAdapter xmlSourceAdapter) {
        this.restSourceAdapter = restSourceAdapter;
        this.xmlSourceAdapter = xmlSourceAdapter;
    }

    /**
     * GET /api/status/rest — is the REST mock service reachable?
     */
    @GetMapping("/rest")
    public ResponseEntity<Map<String, String>> restStatus() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("source", "rest");

        try {
            boolean healthy = restSourceAdapter.checkHealth();
            result.put("status", healthy ? "UP" : "DOWN");
            result.put("message", healthy
                    ? "REST source is reachable"
                    : "REST source returned non-2xx");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", "REST source unreachable: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/status/xml — is the XML mock service reachable?
     */
    @GetMapping("/xml")
    public ResponseEntity<Map<String, String>> xmlStatus() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("source", "xml");

        try {
            boolean healthy = xmlSourceAdapter.checkHealth();
            result.put("status", healthy ? "UP" : "DOWN");
            result.put("message", healthy
                    ? "XML source is reachable"
                    : "XML source returned non-2xx");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", "XML source unreachable: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}
