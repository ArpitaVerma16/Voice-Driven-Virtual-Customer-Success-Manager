package com.vcsm.cqrs.event;

import com.vcsm.cqrs.query.ComplaintReadModel;
import com.vcsm.cqrs.query.ComplaintReadRepository;
import com.vcsm.model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ComplaintEventSynchronizer {

    @Autowired
    private ComplaintReadRepository readRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintCreated(ComplaintCreatedEvent event) {
        Complaint complaint = event.getComplaint();
        ComplaintReadModel readModel = new ComplaintReadModel(
            complaint.getId(),
            complaint.getResidentName(),
            complaint.getDescription(),
            complaint.getCategory() != null ? complaint.getCategory().toString() : "OTHER",
            complaint.getStatus().toString(),
            complaint.getPriority(),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt(),
            complaint.getResolvedBy()
        );
        readRepository.save(readModel);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintUpdated(ComplaintUpdatedEvent event) {
        Complaint complaint = event.getComplaint();
        ComplaintReadModel readModel = readRepository.findById(complaint.getId()).orElse(null);
        if (readModel != null) {
            readModel.setStatus(complaint.getStatus().toString());
            readModel.setPriority(complaint.getPriority());
            readModel.setUpdatedAt(complaint.getUpdatedAt());
            readModel.setResolvedBy(complaint.getResolvedBy());
            readRepository.save(readModel);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintDeleted(ComplaintDeletedEvent event) {
        readRepository.deleteById(event.getComplaintId());
    }

    public static class ComplaintCreatedEvent {
        private final Complaint complaint;

        public ComplaintCreatedEvent(Complaint complaint) {
            this.complaint = complaint;
        }

        public Complaint getComplaint() { return complaint; }
    }

    public static class ComplaintUpdatedEvent {
        private final Complaint complaint;

        public ComplaintUpdatedEvent(Complaint complaint) {
            this.complaint = complaint;
        }

        public Complaint getComplaint() { return complaint; }
    }

    public static class ComplaintDeletedEvent {
        private final Long complaintId;

        public ComplaintDeletedEvent(Long complaintId) {
            this.complaintId = complaintId;
        }

        public Long getComplaintId() { return complaintId; }
    }
}