package com.vcsm.repository;

import com.vcsm.model.VoiceProfile;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoiceProfileRepository extends JpaRepository<VoiceProfile, Long> {
    
    List<VoiceProfile> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<VoiceProfile> findByUserAndActiveTrue(User user);
    
    Optional<VoiceProfile> findByUserAndIsDefaultTrue(User user);
    
    boolean existsByUserAndActiveTrue(User user);
}