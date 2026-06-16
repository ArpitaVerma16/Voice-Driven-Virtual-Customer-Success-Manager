package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private EventCategory category;

    private String location;
    private LocalDateTime eventDate;
    private int maxCapacity;
    private int registrations = 0;
    private boolean active = true;
    private String organizer;
    private LocalDateTime createdAt;

    // Registered users for this event
    @ManyToMany
    @JoinTable(
        name = "event_registrations",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> registeredUsers = new ArrayList<>();

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
    public List<User> getRegisteredUsers() { return registeredUsers; }

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
    public void setRegisteredUsers(List<User> registeredUsers) { this.registeredUsers = registeredUsers; }

    public enum EventCategory { SPORTS, CULTURAL, HEALTH, EDUCATION, ENTERTAINMENT, SOCIAL, OTHER }
}