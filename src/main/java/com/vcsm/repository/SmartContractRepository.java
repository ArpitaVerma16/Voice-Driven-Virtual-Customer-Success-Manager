package com.vcsm.repository;

import com.vcsm.model.SmartContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartContractRepository extends JpaRepository<SmartContract, Long> {
    
    List<SmartContract> findByExecutionStatus(String status);
    
    List<SmartContract> findByComplaintId(Long complaintId);
    
    List<SmartContract> findByEventId(Long eventId);
    
    List<SmartContract> findByAutoExecuteTrueAndExecutionStatus(String status);
}