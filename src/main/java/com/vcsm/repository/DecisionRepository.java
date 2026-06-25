package com.vcsm.repository;

import com.vcsm.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {
    List<Decision> findByDecisionType(String decisionType);
    List<Decision> findByOutcome(String outcome);
    long countByExecutedBy(String executedBy);
}