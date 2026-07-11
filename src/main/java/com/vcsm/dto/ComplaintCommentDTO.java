package com.vcsm.dto;

import java.time.LocalDateTime;

public class ComplaintCommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private String authorName;
    private String authorEmail;
    private boolean isAdmin;

    public ComplaintCommentDTO(Long id, String content, LocalDateTime createdAt, String authorName, String authorEmail, boolean isAdmin) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.isAdmin = isAdmin;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAuthorName() {
        return authorName;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
}
