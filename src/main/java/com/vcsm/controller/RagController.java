package com.vcsm.controller;

import com.vcsm.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            ragService.ingestPdf(file);
            return ResponseEntity.ok("Document ingested successfully into the Vector Store.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to ingest document: " + e.getMessage());
        }
    }
}
