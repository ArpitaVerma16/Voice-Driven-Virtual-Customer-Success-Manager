package com.vcsm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VoiceCloningServiceTest {

    @InjectMocks
    private VoiceCloningService voiceCloningService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void contextLoads() {
        assertNotNull(voiceCloningService);
    }
}
