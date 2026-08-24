package com.nowrongdoor.repository;

import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnifiedResidentRepository extends JpaRepository<UnifiedResident, Long> {

    /** Find by REST ID (exact). */
    Optional<UnifiedResident> findByRestId(String restId);

    /** Find by XML ref (exact). */
    Optional<UnifiedResident> findByXmlRef(String xmlRef);

    /** Full-text substring search across both source representations. */
    @Query("""
        SELECT r FROM UnifiedResident r
        WHERE LOWER(r.restId)        LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlRef)        LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restFirstName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restLastName)  LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(CONCAT(CONCAT(r.restFirstName, ' '), r.restLastName)) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlName)       LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restCity)      LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlTown)       LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restAddressLine) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlAddr)       LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY r.restId ASC NULLS LAST
        """)
    List<UnifiedResident> search(@Param("q") String query);

    /** Full-text search constrained to one match status. */
    @Query("""
        SELECT r FROM UnifiedResident r
        WHERE r.matchStatus = :status
          AND (LOWER(r.restId) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restFirstName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restLastName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(CONCAT(CONCAT(r.restFirstName, ' '), r.restLastName)) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlRef) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restCity) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlTown) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.restAddressLine) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(r.xmlAddr) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY r.restId ASC NULLS LAST
        """)
    List<UnifiedResident> search(@Param("q") String query, @Param("status") MatchStatus status);

    /** Count by match status. */
    long countByMatchStatus(MatchStatus status);

    /** All records with a given match status. */
    List<UnifiedResident> findByMatchStatus(MatchStatus status);
}
