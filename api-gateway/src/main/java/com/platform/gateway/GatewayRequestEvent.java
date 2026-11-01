package com.platform.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Document(indexName = "gateway-logs")

public class GatewayRequestEvent {

    @Id
    private String eventId;
    private String correlationId;
    private String serviceName;
    private String method;
    private String path;
    private int statusCode;
    private long latencyMs;
    private Instant timestamp ;

    public GatewayRequestEvent(
            String correlationId,
            String serviceName,
            String method,
            String path,
            int statusCode,
            long latencyMs,
            Instant timestamp) {

        this.eventId = UUID.randomUUID().toString();
        this.correlationId = correlationId;
        this.serviceName = serviceName;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.timestamp = timestamp == null
                ? Instant.now()
                : timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
