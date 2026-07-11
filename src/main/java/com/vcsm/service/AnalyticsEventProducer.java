package com.vcsm.service;

import com.vcsm.config.RabbitMQConfig;
import com.vcsm.dto.InteractionCompletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public AnalyticsEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEvent(InteractionCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
    }
}
