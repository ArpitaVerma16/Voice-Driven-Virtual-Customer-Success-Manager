package com.vcsm.repository;

import com.vcsm.model.CustomerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerSessionRepository extends JpaRepository<CustomerSession, String> {

    List<CustomerSession> findByCustomerIdOrderByStartedAtDesc(String customerId);

    @Query("""
        SELECT cs FROM CustomerSession cs
        WHERE cs.customerId = :customerId AND cs.isArchived = false
        ORDER BY cs.startedAt DESC LIMIT 50
    """)
    List<CustomerSession> findRecentSessionsForCustomer(@Param("customerId") String customerId);

    @Query("""
        SELECT cs FROM CustomerSession cs
        WHERE cs.startedAt < :cutoffDate AND cs.isArchived = false
        ORDER BY cs.startedAt ASC
    """)
    List<CustomerSession> findSessionsEligibleForArchival(@Param("cutoffDate") LocalDateTime cutoffDate);

    long countByCustomerId(String customerId);
}
