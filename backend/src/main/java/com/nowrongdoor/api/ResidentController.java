package com.nowrongdoor.api;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {
    private final UnifiedResidentRepository residentRepository;

    public ResidentController(UnifiedResidentRepository residentRepository) {
        this.residentRepository = residentRepository;
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(required = false) MatchStatus status) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<UnifiedResident> residents = status == null
                ? residentRepository.search(normalizedQuery)
                : residentRepository.search(normalizedQuery, status);
        return new SearchResponse(residents.stream().map(ResidentResponse::from).toList(), residents.size());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResidentResponse> detail(@PathVariable String id) {
        UnifiedResident resident = residentRepository.findByRestId(id)
                .or(() -> residentRepository.findByXmlRef(id))
                .or(() -> parseDatabaseId(id))
                .orElse(null);
        return resident == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(ResidentResponse.from(resident));
    }

    private java.util.Optional<UnifiedResident> parseDatabaseId(String value) {
        try {
            return residentRepository.findById(Long.valueOf(value));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }
}
