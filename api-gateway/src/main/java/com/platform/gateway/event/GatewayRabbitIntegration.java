package com.platform.gateway.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.gateway.GatewayRequestEvent;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class GatewayRabbitIntegration {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GatewayRabbitIntegration.class);

    private static final String QUEUE = "gateway-alerts";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public GatewayRabbitIntegration(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Queue gatewayAlertsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Async("gatewayEventExecutor")
    @EventListener
    public void publishFailureEvent(GatewayRequestEvent event) {
        if (event.getStatusCode() < 500) {
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(QUEUE, message);

            log.info("Gateway alert queued: {}", event.getEventId());
        } catch (Exception ex) {
            log.error("Could not publish gateway alert to RabbitMQ", ex);
        }
    }

    @RabbitListener(queues = QUEUE)
    public void consumeAlert(String message) {
        log.warn("Gateway alert received from RabbitMQ: {}", message);
    }
}
