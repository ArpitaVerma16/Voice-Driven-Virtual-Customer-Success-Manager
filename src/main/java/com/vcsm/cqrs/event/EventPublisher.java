package com.vcsm.cqrs.event;

import com.vcsm.model.Complaint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishComplaintCreated(Complaint complaint) {
        applicationEventPublisher.publishEvent(new ComplaintEventSynchronizer.ComplaintCreatedEvent(complaint));
    }

    public void publishComplaintUpdated(Complaint complaint) {
        applicationEventPublisher.publishEvent(new ComplaintEventSynchronizer.ComplaintUpdatedEvent(complaint));
    }

    public void publishComplaintDeleted(Long complaintId) {
        applicationEventPublisher.publishEvent(new ComplaintEventSynchronizer.ComplaintDeletedEvent(complaintId));
    }
}