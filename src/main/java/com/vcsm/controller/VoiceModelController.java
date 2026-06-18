package com.vcsm.controller;

import com.vcsm.dto.VoiceModelUploadResponse;
import com.vcsm.service.VoiceModelRegistryService;
import com.vcsm.service.VoiceModelStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping({"/voice-model", "/api/voice-model"})
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "voice.model.s3.bucket")
public class VoiceModelController {

    private final VoiceModelStorageService storageService;
    private final VoiceModelRegistryService registryService;

    public VoiceModelController(VoiceModelStorageService storageService, VoiceModelRegistryService registryService) {
        this.storageService = storageService;
        this.registryService = registryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceModelUploadResponse> uploadVoiceModel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return badRequest("A non-empty .zip voice model file is required.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            return badRequest("Only .zip voice model archives are supported.");
        }

        try {
            VoiceModelStorageService.StoredVoiceModel storedModel = storageService.store(file);
            registryService.activate(storedModel);
            return ResponseEntity.status(HttpStatus.CREATED).body(new VoiceModelUploadResponse(
                    true,
                    "Voice model uploaded successfully.",
                    storedModel.modelKey(),
                    storedModel.bucket()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VoiceModelUploadResponse(
                    false,
                    ex.getMessage(),
                    null,
                    null
            ));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new VoiceModelUploadResponse(
                    false,
                    "Failed to upload voice model. Please try again.",
                    null,
                    null
            ));
        }
    }

    private ResponseEntity<VoiceModelUploadResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new VoiceModelUploadResponse(false, message, null, null));
    }
}
