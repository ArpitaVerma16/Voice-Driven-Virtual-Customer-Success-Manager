package com.vcsm.repository;

import com.vcsm.model.IvrSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IvrSessionRepository extends JpaRepository<IvrSession, Long> {
    Optional<IvrSession> findByUserEmail(String userEmail);
    void deleteByUserEmail(String userEmail);
}
