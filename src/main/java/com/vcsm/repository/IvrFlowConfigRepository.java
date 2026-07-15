package com.vcsm.repository;

import com.vcsm.model.IvrFlowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IvrFlowConfigRepository extends JpaRepository<IvrFlowConfig, Long> {
    Optional<IvrFlowConfig> findFirstByIsActiveTrueOrderByUpdatedAtDesc();
}
