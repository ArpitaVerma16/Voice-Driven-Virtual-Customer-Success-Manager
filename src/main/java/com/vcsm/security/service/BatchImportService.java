package com.vcsm.security.service;

public class BatchImportService {
    
}
package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.model.batch.BatchImportJob;
import com.vcsm.model.batch.BatchImportRecord;
import com.vcsm.repository.BatchImportJobRepository;
import com.vcsm.repository.BatchImportRecordRepository;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchImportService {

    private final BatchImportJobRepository jobRepository;
    private final BatchImportRecordRepository recordRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintService complaintService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    // Track active jobs
    private final Map<String, Boolean> activeJobs = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> jobProgress = new ConcurrentHashMap<>();

    /**
     * Start a batch import job
     */
    @Transactional
    public BatchImportJob startBatchImport(MultipartFile file, String userId, String jobName, Integer batchSize) {
        try {
            log.info("📤 Starting batch import for user: {}", userId);

            // Create job
            BatchImportJob job = BatchImportJob.builder()
                .jobId(UUID.randomUUID().toString())
                .jobName(jobName != null ? jobName : "Bulk Import " + LocalDateTime.now())
                .userId(userId)
                .status(BatchImportJob.JobStatus.PENDING)
                .totalRecords(0)
                .processedRecords(0)
                .successfulRecords(0)
                .failedRecords(0)
                .progressPercentage(0)
                .fileName(file.getOriginalFilename())
                .fileType(getFileType(file.getOriginalFilename()))
                .batchSize(batchSize != null ? batchSize : 100)
                .createdAt(LocalDateTime.now())
                .metadata(objectMapper.writeValueAsString(Map.of(
                    "fileSize", file.getSize(),
                    "contentType", file.getContentType()
                )))
                .build();

            // Parse file to count records
            int totalRecords = countRecords(file);
            job.setTotalRecords(totalRecords);

            BatchImportJob savedJob = jobRepository.save(job);

            // Initialize progress tracking
            jobProgress.put(savedJob.getJobId(), new AtomicInteger(0));
            activeJobs.put(savedJob.getJobId(), true);

            // Start async processing
            processBatchImportAsync(savedJob.getId(), file, userId);

            log.info("✅ Batch import job created: {}", savedJob.getJobId());
            return savedJob;

        } catch (Exception e) {
            log.error("Failed to start batch import", e);
            throw new RuntimeException("Failed to start batch import", e);
        }
    }

    /**
     * Async processing of batch import
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processBatchImportAsync(Long jobId, MultipartFile file, String userId) {
        log.info("🔄 Processing batch import job: {}", jobId);

        try {
            BatchImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            // Update status
            job.setStatus(BatchImportJob.JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            // Parse and process records
            List<Map<String, String>> records = parseFile(file);
            int totalRecords = records.size();
            job.setTotalRecords(totalRecords);

            // Process in batches
            int batchSize = job.getBatchSize();
            int batchNumber = 0;
            int successful = 0;
            int failed = 0;
            int processed = 0;

            for (int i = 0; i < records.size(); i += batchSize) {
                if (!activeJobs.getOrDefault(job.getJobId(), true)) {
                    log.info("⏹️ Job cancelled: {}", job.getJobId());
                    job.setStatus(BatchImportJob.JobStatus.CANCELLED);
                    job.setCompletedAt(LocalDateTime.now());
                    jobRepository.save(job);
                    return CompletableFuture.completedFuture(null);
                }

                batchNumber++;
                int end = Math.min(i + batchSize, records.size());
                List<Map<String, String>> batch = records.subList(i, end);

                log.info("📦 Processing batch {}: {} records", batchNumber, batch.size());

                // Process batch
                BatchResult batchResult = processBatch(job, batch, batchNumber, userId);

                successful += batchResult.successCount;
                failed += batchResult.failCount;
                processed += batchResult.totalCount;

                // Update job progress
                job.setProcessedRecords(processed);
                job.setSuccessfulRecords(successful);
                job.setFailedRecords(failed);
                job.setCurrentBatchNumber(batchNumber);

                int progress = (int) ((double) processed / totalRecords * 100);
                job.setProgressPercentage(Math.min(progress, 100));
                jobProgress.get(job.getJobId()).set(progress);

                jobRepository.save(job);

                // Send progress update
                sendProgressUpdate(job);

                log.info("📊 Progress: {}% ({}/{})", progress, processed, totalRecords);
            }

            // Finalize job
            job.setStatus(failed > 0 ? BatchImportJob.JobStatus.PARTIAL : BatchImportJob.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setProcessingTimeMs(java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis());
            job.setProgressPercentage(100);
            jobRepository.save(job);

            // Send completion notification
            sendCompletionNotification(job);

            log.info("✅ Batch import completed: Job ID={}, Success={}, Failed={}",
                job.getJobId(), successful, failed);

            // Cleanup
            activeJobs.remove(job.getJobId());
            jobProgress.remove(job.getJobId());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Batch import failed", e);
            handleJobFailure(jobId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process a single batch
     */
    @Transactional
    public BatchResult processBatch(BatchImportJob job, List<Map<String, String>> records, 
                                   int batchNumber, String userId) {
        int successCount = 0;
        int failCount = 0;
        List<BatchImportRecord> batchRecords = new ArrayList<>();

        User user = userRepository.findByEmail(userId).orElse(null);

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> row = records.get(i);
            int rowNumber = (batchNumber - 1) * job.getBatchSize() + i + 1;

            try {
                // Validate and create complaint
                Complaint complaint = createComplaintFromRow(row, user);
                
                // Save complaint
                Complaint savedComplaint = complaintService.fileComplaint(complaint);

                // Create success record
                BatchImportRecord record = BatchImportRecord.builder()
                    .job(job)
                    .rowNumber(rowNumber)
                    .rawData(objectMapper.writeValueAsString(row))
                    .processedData(objectMapper.writeValueAsString(savedComplaint))
                    .status(BatchImportRecord.RecordStatus.SUCCESS)
                    .complaintId(savedComplaint.getId())
                    .processedAt(LocalDateTime.now())
                    .build();

                successCount++;
                batchRecords.add(record);

            } catch (Exception e) {
                log.error("Failed to process row {}: {}", rowNumber, e.getMessage());

                // Create failure record
                BatchImportRecord record = BatchImportRecord.builder()
                    .job(job)
                    .rowNumber(rowNumber)
                    .rawData(objectMapper.writeValueAsString(row))
                    .status(BatchImportRecord.RecordStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .validationErrors(objectMapper.writeValueAsString(getValidationErrors(row)))
                    .processedAt(LocalDateTime.now())
                    .build();

                failCount++;
                batchRecords.add(record);
            }
        }

        // Save batch records
        recordRepository.saveAll(batchRecords);

        return BatchResult.builder()
            .totalCount(records.size())
            .successCount(successCount)
            .failCount(failCount)
            .build();
    }

    /**
     * Parse CSV file
     */
    private List<Map<String, String>> parseFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        if (fileName.endsWith(".csv")) {
            return parseCSV(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        } else if (fileName.endsWith(".json")) {
            return parseJSON(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + fileName);
        }
    }

    /**
     * Parse CSV file
     */
    private List<Map<String, String>> parseCSV(MultipartFile file) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            List<String> headers = parser.getHeaderNames();
            
            for (CSVRecord csvRecord : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, csvRecord.get(header));
                }
                records.add(row);
            }
        }
        
        return records;
    }

    /**
     * Parse Excel file
     */
    private List<Map<String, String>> parseExcel(MultipartFile file) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            if (headerRow == null) {
                throw new IllegalArgumentException("No header row found");
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), cell != null ? cell.toString() : "");
                }
                records.add(rowData);
            }
        }
        
        return records;
    }

    /**
     * Parse JSON file
     */
    private List<Map<String, String>> parseJSON(MultipartFile file) throws Exception {
        String content = new String(file.getBytes());
        List<Map<String, String>> records = new ArrayList<>();
        
        // Simple JSON parsing - in production use proper JSON parser
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = objectMapper.readValue(content, List.class);
        
        for (Map<String, Object> jsonRecord : jsonRecords) {
            Map<String, String> row = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : jsonRecord.entrySet()) {
                row.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
            records.add(row);
        }
        
        return records;
    }

    /**
     * Count records without parsing full content
     */
    private int countRecords(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return 0;

        if (fileName.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                return (int) reader.lines().count() - 1; // Exclude header
            }
        } else {
            // For other formats, parse to count
            return parseFile(file).size();
        }
    }

    /**
     * Create complaint from CSV/Excel row
     */
    private Complaint createComplaintFromRow(Map<String, String> row, User user) {
        Complaint complaint = new Complaint();
        
        // Required fields
        complaint.setDescription(getRequiredValue(row, "description"));
        complaint.setCategory(Complaint.ComplaintCategory.valueOf(
            getRequiredValue(row, "category").toUpperCase()
        ));
        
        // Optional fields
        complaint.setResidentName(row.getOrDefault("residentName", 
            user != null ? user.getName() : "Unknown"));
        complaint.setApartmentNumber(row.getOrDefault("apartmentNumber", 
            user != null ? user.getApartmentNumber() : null));
        complaint.setContactEmail(row.getOrDefault("contactEmail", 
            user != null ? user.getEmail() : null));
        
        // Set defaults
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);
        complaint.setCreatedAt(LocalDateTime.now());
        
        return complaint;
    }

    /**
     * Get required value or throw exception
     */
    private String getRequiredValue(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required field missing: " + key);
        }
        return value.trim();
    }

    /**
     * Validate row and return errors
     */
    private Map<String, String> getValidationErrors(Map<String, String> row) {
        Map<String, String> errors = new LinkedHashMap<>();
        
        if (!row.containsKey("description") || row.get("description").trim().isEmpty()) {
            errors.put("description", "Description is required");
        }
        
        if (!row.containsKey("category") || row.get("category").trim().isEmpty()) {
            errors.put("category", "Category is required");
        }
        
        try {
            if (row.containsKey("category") && !row.get("category").trim().isEmpty()) {
                Complaint.ComplaintCategory.valueOf(row.get("category").toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            errors.put("category", "Invalid category: " + row.get("category"));
        }
        
        return errors;
    }

    /**
     * Get file type from filename
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        if (fileName.endsWith(".csv")) return "CSV";
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) return "EXCEL";
        if (fileName.endsWith(".json")) return "JSON";
        return "UNKNOWN";
    }

    /**
     * Get job status
     */
    public BatchImportJob getJobStatus(String jobId) {
        return jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
    }

    /**
     * Get job progress
     */
    public int getJobProgress(String jobId) {
        return jobProgress.getOrDefault(jobId, new AtomicInteger(0)).get();
    }

    /**
     * Get paginated jobs for user
     */
    public Page<BatchImportJob> getJobsForUser(String userId, Pageable pageable) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get all jobs (admin)
     */
    public Page<BatchImportJob> getAllJobs(Pageable pageable) {
        return jobRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get records for a job
     */
    public Page<BatchImportRecord> getJobRecords(Long jobId, Pageable pageable) {
        return recordRepository.findByJobId(jobId, pageable);
    }

    /**
     * Cancel a job
     */
    @Transactional
    public boolean cancelJob(String jobId) {
        if (activeJobs.containsKey(jobId)) {
            activeJobs.put(jobId, false);
            
            BatchImportJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
            job.setStatus(BatchImportJob.JobStatus.CANCELLED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            log.info("⏹️ Job cancelled: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Retry failed records for a job
     */
    @Transactional
    public BatchImportJob retryFailedRecords(Long jobId) {
        BatchImportJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        List<BatchImportRecord> failedRecords = recordRepository.findByJobIdAndStatus(jobId, 
            BatchImportRecord.RecordStatus.FAILED);

        if (failedRecords.isEmpty()) {
            throw new RuntimeException("No failed records to retry");
        }

        log.info("🔄 Retrying {} failed records for job: {}", failedRecords.size(), jobId);

        // Reset status for retry
        for (BatchImportRecord record : failedRecords) {
            record.setStatus(BatchImportRecord.RecordStatus.PENDING);
            record.setErrorMessage(null);
            recordRepository.save(record);
        }

        // Re-process failed records
        List<Map<String, String>> records = failedRecords.stream()
            .map(record -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> data = objectMapper.readValue(record.getRawData(), Map.class);
                    return data;
                } catch (Exception e) {
                    log.error("Failed to parse record: {}", record.getId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!records.isEmpty()) {
            BatchResult result = processBatch(job, records, job.getCurrentBatchNumber() + 1, job.getUserId());
            
            // Update job status
            job.setStatus(result.failCount > 0 ? BatchImportJob.JobStatus.PARTIAL : BatchImportJob.JobStatus.COMPLETED);
            job.setSuccessfulRecords(job.getSuccessfulRecords() + result.successCount);
            job.setFailedRecords(job.getFailedRecords() + result.failCount);
            job.setProcessedRecords(job.getProcessedRecords() + result.totalCount);
            jobRepository.save(job);
        }

        return job;
    }

    /**
     * Get summary statistics
     */
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("totalJobs", jobRepository.count());
        stats.put("pendingJobs", jobRepository.countByStatus(BatchImportJob.JobStatus.PENDING));
        stats.put("processingJobs", jobRepository.countByStatus(BatchImportJob.JobStatus.PROCESSING));
        stats.put("completedJobs", jobRepository.countByStatus(BatchImportJob.JobStatus.COMPLETED));
        stats.put("failedJobs", jobRepository.countByStatus(BatchImportJob.JobStatus.FAILED));
        stats.put("partialJobs", jobRepository.countByStatus(BatchImportJob.JobStatus.PARTIAL));
        stats.put("activeJobs", activeJobs.size());
        
        return stats;
    }

    /**
     * Handle job failure
     */
    private void handleJobFailure(Long jobId, Exception e) {
        try {
            BatchImportJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus(BatchImportJob.JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                activeJobs.remove(job.getJobId());
                jobProgress.remove(job.getJobId());
            }
        } catch (Exception ex) {
            log.error("Failed to update job status", ex);
        }
    }

    /**
     * Send progress update via WebSocket
     */
    private void sendProgressUpdate(BatchImportJob job) {
        // Implementation depends on WebSocket setup
        // Can be integrated with existing notification service
        try {
            Map<String, Object> progress = Map.of(
                "jobId", job.getJobId(),
                "progress", job.getProgressPercentage(),
                "processed", job.getProcessedRecords(),
                "total", job.getTotalRecords(),
                "success", job.getSuccessfulRecords(),
                "failed", job.getFailedRecords(),
                "status", job.getStatus().toString()
            );
            // Send via WebSocket or notification service
        } catch (Exception e) {
            log.warn("Failed to send progress update", e);
        }
    }

    /**
     * Send completion notification
     */
    private void sendCompletionNotification(BatchImportJob job) {
        try {
            String message = String.format(
                "Batch import completed: %d records processed, %d successful, %d failed",
                job.getProcessedRecords(),
                job.getSuccessfulRecords(),
                job.getFailedRecords()
            );
            
            notificationService.sendGlobalNotification(
                notificationService.createNotification(
                    null,
                    "Batch Import Completed",
                    message,
                    job.getFailedRecords() > 0 ? "WARNING" : "SUCCESS"
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send completion notification", e);
        }
    }

    // Inner class for batch results
    @lombok.Builder
    @lombok.Data
    public static class BatchResult {
        private int totalCount;
        private int successCount;
        private int failCount;
    }
}