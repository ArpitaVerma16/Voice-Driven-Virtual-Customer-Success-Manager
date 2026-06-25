package com.vcsm.repository;

import com.vcsm.model.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    
    List<ModelVersion> findByModelNameOrderByCreatedAtDesc(String modelName);
    
    Optional<ModelVersion> findByModelNameAndIsActiveTrue(String modelName);
    
    Optional<ModelVersion> findByModelNameAndVersion(String modelName, String version);
    
    List<ModelVersion> findByIsDeployedTrue();
}