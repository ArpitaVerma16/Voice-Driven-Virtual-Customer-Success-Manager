// model/elasticsearch/ComplaintDocument.java
package com.vcsm.model.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vcsm.model.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "complaints")
public class ComplaintDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long complaintId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String residentName;

    @Field(type = FieldType.Text, analyzer = "standard", fielddata = true)
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Keyword)
    private String apartmentNumber;

    @Field(type = FieldType.Keyword)
    private String contactEmail;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String resolutionNotes;

    @Field(type = FieldType.Date)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime resolvedAt;

    @Field(type = FieldType.Integer)
    private Integer responseTimeHours;

    @Field(type = FieldType.Object)
    private List<CommentData> comments;

    @Field(type = FieldType.Text)
    private List<String> keywords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentData {
        private String commenter;
        private String text;
        private LocalDateTime timestamp;
    }

    // Convert from Complaint entity
    public static ComplaintDocument fromComplaint(Complaint complaint) {
        return ComplaintDocument.builder()
            .complaintId(complaint.getId())
            .residentName(complaint.getResidentName())
            .description(complaint.getDescription())
            .status(complaint.getStatus().toString())
            .category(complaint.getCategory().toString())
            .priority(complaint.getPriority() != null ? complaint.getPriority().toString() : "MEDIUM")
            .apartmentNumber(complaint.getApartmentNumber())
            .contactEmail(complaint.getContactEmail())
            .createdAt(complaint.getCreatedAt())
            .updatedAt(complaint.getUpdatedAt())
            .keywords(extractKeywords(complaint.getDescription()))
            .build();
    }

    private static List<String> extractKeywords(String text) {
        // Simple keyword extraction - can be enhanced with NLP
        return List.of(text.toLowerCase().split(" "));
    }
}