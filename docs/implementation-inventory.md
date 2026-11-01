# Implementation inventory

## API Gateway

- `GatewayConfig`: User and Order routes, OpenAPI forwarding, Redis rate limiting, circuit breaker, GET-only retry, and load-balanced service discovery.
- `SecurityConfig`, `JwtService`, `JwtAuthenticationManager`: JWT verification and route authorization.
- `FallbackController`, `GatewayExceptionHandler`: consistent fallback and reactive JSON exception responses.
- `CorrelationIdGlobalFilter`, `GatewayMetricsFilter`, `GatewayObservabilityController`, `Service`: correlation IDs, request telemetry, in-memory observability summaries, and Micrometer metrics.
- `GatewayAsyncConfiguration`, `Producer`, `Consumer`, `KafkaTopicConfiguration`: asynchronous request event publishing, Kafka topic provisioning, and Elasticsearch indexing.
- `GatewayRabbitIntegration`: durable RabbitMQ queue for 5xx gateway alerts.
- `GatewayEventBloomFilter`: in-memory duplicate pre-check; Elasticsearch remains the durable source of truth.
- `RateLimitConfig`, `BulkheadConfiguration`, `CircuitBreakerEventLogger`: Redis IP rate limiting and Resilience4j resilience configuration.

## User Service

- Signup/login, BCrypt password hashing, JWT creation, JWT filter, MongoDB repository, DTO validation, and error advice.

## Order Service

- MongoDB-backed create/read/update/delete operations, validated request DTOs, response mapping, and error advice.

## Infrastructure

- `docker-compose.yml`: MongoDB, Redis, Kafka, RabbitMQ, Elasticsearch, User Service and replica, Order Service, Gateway, Prometheus and Grafana.
- `application-docker.yml`: container DNS and internal ports.
- `prometheus.yml`, Grafana datasource/dashboard provisioning, smoke-test script, README, and architecture documentation.

## Deliberate local-development limitations

The Bloom filter is memory-only; a restart clears it, while Elasticsearch's event index persists. The provided stack uses single-node infrastructure and development defaults; configure strong secrets and add TLS/authentication before exposing it beyond a trusted local network.
