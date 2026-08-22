package com.nowrongdoor.api;

import com.nowrongdoor.ingestion.IngestionResult;
import com.nowrongdoor.ingestion.IngestionService;
import com.nowrongdoor.model.IngestionRun;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<IngestionResult> ingest() {
        return ResponseEntity.ok(ingestionService.ingest());
    }

    @GetMapping("/status")
    public ResponseEntity<IngestionResult> latestStatus() {
        return ingestionService.latestRun()
                .map(run -> ResponseEntity.ok(IngestionResult.from(run)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
