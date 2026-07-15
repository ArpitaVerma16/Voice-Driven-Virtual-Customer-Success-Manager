package com.vcsm.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "analytics.interaction.queue";
    public static final String EXCHANGE_NAME = "analytics.exchange";
    public static final String ROUTING_KEY = "analytics.routing.key";

    @Bean
    public Queue analyticsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange analyticsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, DirectExchange analyticsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(analyticsExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
