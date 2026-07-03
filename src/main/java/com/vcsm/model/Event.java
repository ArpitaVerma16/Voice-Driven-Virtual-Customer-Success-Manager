package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event name is required")
    private String name;

    @Size(max = 2000)
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private EventCategory category;

    @Size(max = 500)
    private String location;
    @NotNull
    private LocalDateTime eventDate;
    @Min(1)
    @Max(10000)
    private int maxCapacity;
    private int registrations = 0;
    private boolean active = true;
    private String organizer;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<EventRegistration> registrationList = new ArrayList<>();



    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ---- Getters ----
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public EventCategory getCategory() { return category; }
    public String getLocation() { return location; }
    public LocalDateTime getEventDate() { return eventDate; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getRegistrations() { return registrations; }
    public boolean isActive() { return active; }
    public String getOrganizer() { return organizer; }
    public LocalDateTime getCreatedAt() { return createdAt; }


    // ---- Setters ----
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(EventCategory category) { this.category = category; }
    public void setLocation(String location) { this.location = location; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setRegistrations(int registrations) { this.registrations = registrations; }
    public void setActive(boolean active) { this.active = active; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


    public enum EventCategory { SPORTS, CULTURAL, HEALTH, EDUCATION, ENTERTAINMENT, SOCIAL, OTHER }
}