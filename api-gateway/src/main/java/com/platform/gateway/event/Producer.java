package com.platform.gateway.event;

import com.platform.gateway.GatewayRequestEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class Producer {

    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    private static final String TOPIC = "gateway-request-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public Producer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("gatewayEventExecutor")
    @EventListener
    public void publishEvent(GatewayRequestEvent event) {

        try {
            String message =
                    objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    TOPIC,
                    event.getEventId(),
                    message
            ).addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

                @Override
                public void onSuccess(SendResult<String, String> result) {
                    log.info(
                            "Event published: topic={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().offset()
                    );
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Kafka publishing failed", ex);
                }
            });

        } catch (JsonProcessingException ex) {
            log.error("Event serialization failed", ex);
        }
    }
}
