package com.vcsm.repository;

import com.vcsm.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(Complaint.ComplaintStatus status);

    List<Complaint> findByResidentName(String residentName);

    List<Complaint> findByResidentUsernameOrderByCreatedAtDesc(String residentUsername);

    Page<Complaint> findByResidentUsername(String residentUsername, Pageable pageable);

    List<Complaint> findByPriority(String priority);

    List<Complaint> findByPriorityOrderByCreatedAtAsc(String priority);

    Optional<Complaint> findByIdAndResidentUsername(Long id, String residentUsername);


    List<Complaint> findByCategory(Complaint.ComplaintCategory category);

    List<Complaint> findByApartmentNumber(String apartmentNumber);

    long countByStatus(Complaint.ComplaintStatus status);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c FROM Complaint c ORDER BY c.createdAt DESC")
    List<Complaint> findAllOrderByCreatedAtDesc();

    @Query("SELECT c.priority, COUNT(c) FROM Complaint c GROUP BY c.priority")
    List<Object[]> countByPriority();

    @Query("SELECT c.id FROM Complaint c")
List<Long> findAllIds();

@Query("SELECT c.id FROM Complaint c WHERE c.status = :status")
List<Long> findIdsByStatus(@Param("status") Complaint.ComplaintStatus status);



    Page<Complaint> findAll(Pageable pageable);
Page<Complaint> findByResidentUsername(String username, Pageable pageable);
}
