package com.vcsm.repository;

import com.vcsm.model.batch.BatchImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchImportJobRepository extends JpaRepository<BatchImportJob, Long> {

    Optional<BatchImportJob> findByJobId(String jobId);
    
    Page<BatchImportJob> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    Page<BatchImportJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    long countByStatus(BatchImportJob.JobStatus status);
    
    List<BatchImportJob> findByStatus(BatchImportJob.JobStatus status);
    
    @Query("SELECT COUNT(j) FROM BatchImportJob j WHERE j.status = 'PROCESSING'")
    long countActiveJobs();
    
    @Query("SELECT j.userId, COUNT(j) FROM BatchImportJob j GROUP BY j.userId")
    List<Object[]> countByUser();
}