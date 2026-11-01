# Architecture

```mermaid
flowchart LR
    Client[Client / Postman / Swagger] --> GW[Spring Cloud Gateway :8080]
    GW --> RL[Redis Rate Limiter]
    GW --> LB{Load Balancer}
    LB --> U1[User Service :8080]
    LB --> U2[User Service Replica :8080]
    GW --> O[Order Service :8082]
    GW --> CB[Circuit Breaker / Retry / Bulkhead]
    GW --> MET[Micrometer + Actuator]
    MET --> PROM[Prometheus]
    PROM --> GRAF[Grafana]
    GW --> EVT[Gateway Request Event]
    EVT --> KPROD[Kafka Producer]
    KPROD --> K[Kafka Topic: gateway-request-events]
    K --> KCON[Kafka Consumer]
    KCON --> ES[Elasticsearch: gateway-logs]
    EVT --> RAB[5xx Alert Publisher]
    RAB --> RMQ[RabbitMQ: gateway-alerts]
    U1 --> MDB[(MongoDB)]
    U2 --> MDB
    O --> MDB
```

## Request lifecycle

- Gateway security validates a Bearer JWT using the shared signing secret.
- RedisRateLimiter applies an IP-keyed token bucket to User and Order routes.
- Spring Cloud LoadBalancer selects an instance from simple discovery entries.
- Circuit Breaker and Retry filters protect routed GET traffic; the fallback controller returns a JSON 503 when the circuit breaker opens.
- CorrelationIdGlobalFilter propagates `X-Correlation-ID`, measures latency, records in-memory observability counters, and publishes a `GatewayRequestEvent`.
- Kafka persists request-event messages for asynchronous processing. Consumer checks the Bloom filter and verifies possible duplicates against Elasticsearch before saving.
- RabbitMQ receives 5xx event alerts. The current consumer logs alerts; it is a demonstration notification sink, not an email/SMS delivery integration.
- GatewayMetricsFilter emits custom Micrometer counters/timers. Actuator exposes them for Prometheus.

## Docker network endpoints

Within Compose, services use DNS names: `redis:6379`, `kafka:29092`, `rabbitmq:5672`, `elasticsearch:9200`, `user-service:8080`, `user-service-replica:8080`, and `order-service:8082`. Host access uses the published ports in the README.
