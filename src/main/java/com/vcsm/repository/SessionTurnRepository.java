package com.vcsm.repository;

import com.vcsm.model.SessionTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionTurnRepository extends JpaRepository<SessionTurn, String> {

    List<SessionTurn> findBySessionIdOrderByTurnIndexAsc(String sessionId);
}
