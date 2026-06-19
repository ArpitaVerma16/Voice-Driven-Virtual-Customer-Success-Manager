package com.vcsm.repository;

import com.vcsm.model.VoicePrint;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VoicePrintRepository extends JpaRepository<VoicePrint, Long> {
    Optional<VoicePrint> findByUser(User user);
    Optional<VoicePrint> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}