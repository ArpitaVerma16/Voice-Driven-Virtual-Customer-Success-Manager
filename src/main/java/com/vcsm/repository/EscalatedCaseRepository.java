package com.vcsm.repository;

import com.vcsm.model.EscalatedCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EscalatedCaseRepository extends JpaRepository<EscalatedCase, Long> {
    
    List<EscalatedCase> findByResolved(boolean resolved);
    
    List<EscalatedCase> findByAdminNotified(boolean adminNotified);
}