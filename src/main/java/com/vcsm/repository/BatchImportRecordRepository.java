package com.vcsm.repository;

import com.vcsm.model.batch.BatchImportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchImportRecordRepository extends JpaRepository<BatchImportRecord, Long> {

    Page<BatchImportRecord> findByJobId(Long jobId, Pageable pageable);
    
    List<BatchImportRecord> findByJobIdAndStatus(Long jobId, BatchImportRecord.RecordStatus status);
    
    long countByJobIdAndStatus(Long jobId, BatchImportRecord.RecordStatus status);
}