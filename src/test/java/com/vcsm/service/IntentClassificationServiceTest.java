package com.vcsm.service;

import com.vcsm.dto.IntentResult;
import com.vcsm.model.CustomerIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntentClassificationServiceTest {

    private IntentClassificationService service;

    @BeforeEach
    public void setUp() {
        service = new IntentClassificationService();
    }

    @Test
    public void testBillingInquiryIntent() {
        IntentResult result = service.classify("Can you help me with my invoice?");
        assertEquals(CustomerIntent.BILLING_INQUIRY, result.getClassifiedIntent());
        assertTrue(result.getConfidence() >= 0.5);
        assertTrue(result.isConfident());
    }

    @Test
    public void testTechnicalSupportIntent() {
        IntentResult result = service.classify("The system is crashing and showing errors");
        assertEquals(CustomerIntent.TECHNICAL_SUPPORT, result.getClassifiedIntent());
        assertTrue(result.isConfident());
    }

    @Test
    public void testAccountManagementIntent() {
        IntentResult result = service.classify("I need to reset my password");
        assertEquals(CustomerIntent.ACCOUNT_MANAGEMENT, result.getClassifiedIntent());
        assertTrue(result.isConfident());
    }

    @Test
    public void testCancellationRequestIntent() {
        IntentResult result = service.classify("I want to cancel my subscription");
        assertEquals(CustomerIntent.CANCELLATION_REQUEST, result.getClassifiedIntent());
        assertTrue(result.isConfident());
    }

    @Test
    public void testFeatureRequestIntent() {
        IntentResult result = service.classify("Could you add this feature?");
        assertEquals(CustomerIntent.FEATURE_REQUEST, result.getClassifiedIntent());
        assertTrue(result.isConfident());
    }

    @Test
    public void testGeneralInquiryFallback() {
        IntentResult result = service.classify("Hello there");
        assertEquals(CustomerIntent.GENERAL_INQUIRY, result.getClassifiedIntent());
        assertFalse(result.isConfident());
    }

    @Test
    public void testAmbiguousInputFallback() {
        IntentResult result = service.classify("What is this?");
        assertEquals(CustomerIntent.GENERAL_INQUIRY, result.getClassifiedIntent());
    }

    @Test
    public void testEmptyInputHandling() {
        IntentResult result = service.classify("");
        assertEquals(CustomerIntent.GENERAL_INQUIRY, result.getClassifiedIntent());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    public void testNullInputHandling() {
        IntentResult result = service.classify(null);
        assertEquals(CustomerIntent.GENERAL_INQUIRY, result.getClassifiedIntent());
    }

    @Test
    public void testConfidenceScore() {
        IntentResult result = service.classify("I have a billing question about my invoice");
        assertTrue(result.getConfidence() > 0.5);
        assertTrue(result.getConfidence() <= 1.0);
    }
}
