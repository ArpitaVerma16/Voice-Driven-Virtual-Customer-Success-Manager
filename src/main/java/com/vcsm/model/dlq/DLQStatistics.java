package com.vcsm.model.dlq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLQStatistics {
    private Long totalEntries;
    private Long pending;
    private Long retrying;
    private Long resolved;
    private Long failed;
    private Long cancelled;
    private Map<String, Long> byQueue;
    private Map<String, Long> byFailureReason;
    private Double averageRetryCount;
    private Double successRate;
    private Long oldestMessageAge;
    private Long longestRetryTime;
}