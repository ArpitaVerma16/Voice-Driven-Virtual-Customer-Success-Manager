package com.vcsm.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PIIEncryptionServiceTest {

    private PIIEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new PIIEncryptionService();
    }

    @Test
    void testEncryptionKeyGeneration() {
        String key = encryptionService.generateEncryptionKey();
        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    void testMaskPIIField() {
        String masked1 = encryptionService.maskPIIField("1234567890");
        assertEquals("******7890", masked1);

        String masked2 = encryptionService.maskPIIField("12345");
        assertEquals("*****", masked2);

        String masked3 = encryptionService.maskPIIField("ABC");
        assertEquals("***", masked3);
    }

    @Test
    void testMaskPIIFieldShort() {
        String masked = encryptionService.maskPIIField("12");
        assertEquals("**", masked);
    }

    @Test
    void testMaskPIIFieldEmpty() {
        String masked = encryptionService.maskPIIField("");
        assertEquals("", masked);
    }

    @Test
    void testMaskPIIFieldNull() {
        String masked = encryptionService.maskPIIField(null);
        assertNull(masked);
    }

    @Test
    void testEncryptionDisabled() {
        assertFalse(encryptionService.isEncryptionEnabled());
    }
}
