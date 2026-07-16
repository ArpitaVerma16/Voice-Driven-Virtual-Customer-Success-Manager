package com.vcsm.batch;

import com.vcsm.model.batch.BatchImportJob;
import com.vcsm.service.BatchImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class BatchImportTest {

    @Autowired
    private BatchImportService batchImportService;

    @Test
    void shouldStartBatchImport() throws Exception {
        // Create test CSV content
        String csvContent = "description,category,apartmentNumber\n" +
                           "Test complaint 1,MAINTENANCE,A-101\n" +
                           "Test complaint 2,SECURITY,B-202\n" +
                           "Test complaint 3,MAINTENANCE,C-303\n";
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Start import
        BatchImportJob job = batchImportService.startBatchImport(
            file,
            "test@example.com",
            "Test Import",
            10
        );

        assertThat(job).isNotNull();
        assertThat(job.getJobId()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(BatchImportJob.JobStatus.PENDING);
        assertThat(job.getTotalRecords()).isGreaterThan(0);

        // Wait a bit for processing
        Thread.sleep(5000);

        // Check progress
        int progress = batchImportService.getJobProgress(job.getJobId());
        assertThat(progress).isBetween(0, 100);
    }
}