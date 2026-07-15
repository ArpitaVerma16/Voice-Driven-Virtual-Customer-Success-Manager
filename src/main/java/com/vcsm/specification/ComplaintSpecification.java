package com.vcsm.specification;

import com.vcsm.model.Complaint;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ComplaintSpecification {

    public static Specification<Complaint> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("residentName")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern)
            );
        };
    }

    public static Specification<Complaint> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isEmpty()) {
                return cb.conjunction();
            }
            try {
                Complaint.ComplaintStatus complaintStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
                return cb.equal(root.get("status"), complaintStatus);
            } catch (IllegalArgumentException e) {
                return cb.conjunction();
            }
        };
    }

    public static Specification<Complaint> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isEmpty()) {
                return cb.conjunction();
            }
            try {
                Complaint.ComplaintCategory complaintCategory = Complaint.ComplaintCategory.valueOf(category.toUpperCase());
                return cb.equal(root.get("category"), complaintCategory);
            } catch (IllegalArgumentException e) {
                return cb.conjunction();
            }
        };
    }

    public static Specification<Complaint> hasPriority(String priority) {
        return (root, query, cb) -> {
            if (priority == null || priority.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("priority"), priority.toUpperCase());
        };
    }

    public static Specification<Complaint> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}