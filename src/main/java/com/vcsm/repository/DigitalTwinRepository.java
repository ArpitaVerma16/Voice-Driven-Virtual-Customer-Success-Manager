package com.vcsm.repository;

import com.vcsm.model.DigitalTwin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalTwinRepository extends JpaRepository<DigitalTwin, Long> {
    
    List<DigitalTwin> findByStatus(String status);
    
    List<DigitalTwin> findBySourceSystem(String sourceSystem);
}