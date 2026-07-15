package com.vcsm.service;

import com.vcsm.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class SmartLockService {

    private final EmailService emailService;

    @Autowired
    public SmartLockService(EmailService emailService) {
        this.emailService = emailService;
    }

    public String generateGuestPass(User resident, String guestName, int durationHours) {
        // Mock Smart Lock API integration to generate a time-bound access code
        String pinCode = String.format("%06d", new Random().nextInt(1000000));
        
        String subject = "Guest Access Pass Generated: " + guestName;
        String message = String.format(
            "Hello %s,\n\n" +
            "A temporary smart lock guest pass has been successfully generated for %s.\n\n" +
            "--- GUEST PASS DETAILS ---\n" +
            "PIN Code: %s\n" +
            "Valid for: %d hours from now\n" +
            "--------------------------\n\n" +
            "Please share this PIN code securely with your guest.\n\n" +
            "Thank you,\nVirtual Community Management",
            resident.getName(), guestName, pinCode, durationHours
        );

        // Send email to the resident
        emailService.sendSimpleEmail(resident.getEmail(), subject, message);

        return String.format(
            "I have generated a smart lock guest pass for %s valid for %d hours. The 6-digit PIN code is %s. " +
            "I have also sent these details to your registered email address.",
            guestName, durationHours, pinCode
        );
    }
}
