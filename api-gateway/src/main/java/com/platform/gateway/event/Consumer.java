package com.platform.gateway.event;

import com.platform.gateway.GatewayRequestEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.gateway.observability.GatewayEventBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private final ObjectMapper objectMapper;
    private final GatewayEventBloomFilter bloomFilter;
    private final ElasticsearchOperations elasticsearch;

    public Consumer(
            ObjectMapper objectMapper,
            GatewayEventBloomFilter bloomFilter,
            ElasticsearchOperations elasticsearch) {

        this.objectMapper = objectMapper;
        this.bloomFilter = bloomFilter;
        this.elasticsearch = elasticsearch;
    }

    @KafkaListener(
            topics = "gateway-request-events",
            groupId = "api-gateway"
    )
    public void consume(String message) throws Exception {

        GatewayRequestEvent event =
                objectMapper.readValue(
                        message, GatewayRequestEvent.class);

        String eventId = event.getEventId();

        if (eventId == null || eventId.trim().isEmpty()) {
            log.warn("Event rejected: missing event ID");
            return;
        }

        // Ensure the target index exists before the first write.
        org.springframework.data.elasticsearch.core.IndexOperations indexOperations =
                elasticsearch.indexOps(GatewayRequestEvent.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }

        // Check possible duplicate using Bloom Filter
        if (bloomFilter.mightContain(eventId)) {

            GatewayRequestEvent existing =
                    elasticsearch.get(
                            eventId, GatewayRequestEvent.class);

            if (existing != null) {
                log.info("Duplicate event ignored: {}", eventId);
                return;
            }
        }

        // Store event in Elasticsearch
        elasticsearch.save(event);

        // Update Bloom Filter after successful storage
        bloomFilter.add(eventId);

        log.info(
                "Event successfully indexed: {}",
                eventId
        );
    }
}
