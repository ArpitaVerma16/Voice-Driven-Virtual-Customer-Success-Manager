package com.vcsm.repository;

import com.vcsm.model.VenueReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VenueReservationRepository extends JpaRepository<VenueReservation, Long> {
    List<VenueReservation> findByVenueNameIgnoreCaseAndStatus(String venueName, String status);
}
