package com.vcsm.service;

import com.vcsm.config.RabbitMQConfig;
import com.vcsm.dto.InteractionCompletedEvent;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

    private final VoiceAnalyticsService voiceAnalyticsService;
    private final UserRepository userRepository;

    @Autowired
    public AnalyticsEventConsumer(VoiceAnalyticsService voiceAnalyticsService, UserRepository userRepository) {
        this.voiceAnalyticsService = voiceAnalyticsService;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeMessage(InteractionCompletedEvent event) {
        try {
            log.info("Received async analytics event for intent: {}", event.getIntent());

            User user = null;
            if (event.getUserEmail() != null) {
                Optional<User> optionalUser = userRepository.findByEmail(event.getUserEmail());
                if (optionalUser.isPresent()) {
                    user = optionalUser.get();
                }
            }
            
            // Perform the heavy DB logging and any downstream ML API calls asynchronously
            voiceAnalyticsService.logCommand(user, event.getTranscript(), event.getIntent(), event.isSuccess(), event.getResponseTime());
            
        } catch (Exception e) {
            log.error("Error processing analytics event: ", e);
        }
    }
}
