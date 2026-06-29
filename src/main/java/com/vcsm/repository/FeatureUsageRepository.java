
package com.vcsm.repository;

import com.vcsm.model.FeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, Long> {
    
    List<FeatureUsage> findByFeatureName(String featureName);
    
    List<FeatureUsage> findByUserId(Long userId);
    
    @Query("SELECT f.featureName, AVG(f.usageCount) FROM FeatureUsage f GROUP BY f.featureName")
    List<Object[]> getAverageUsageByFeature();
    
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsage f WHERE f.lastUsed > :since GROUP BY f.featureName")
    List<Object[]> getRecentUsage(@Param("since") LocalDateTime since);
    
    @Query("SELECT f.featureName, AVG(f.userRating) FROM FeatureUsage f GROUP BY f.featureName ORDER BY AVG(f.userRating) DESC")
    List<Object[]> getTopRatedFeatures();
}