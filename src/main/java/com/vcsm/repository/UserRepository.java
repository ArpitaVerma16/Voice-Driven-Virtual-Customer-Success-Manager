package com.vcsm.repository;

import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * High-risk residents for the dashboard. Filtering in the database
     * replaces loading the whole users table and filtering in memory.
     */
    java.util.List<User> findByDissatisfactionScoreGreaterThanEqual(double threshold);
}