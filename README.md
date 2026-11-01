# Distributed API Platform

A Java 8 / Spring Boot 2.7.18 multi-module project demonstrating an API Gateway in front of User and Order services, with Redis rate limiting, load balancing, JWT authentication, resilience patterns, event-driven request telemetry, Elasticsearch storage, Prometheus metrics, and Grafana dashboards.

## Modules

- `api-gateway`: Spring Cloud Gateway, JWT validation, routing, Redis rate limiting, load balancing, Resilience4j, Kafka producer/consumer, RabbitMQ alerts, Elasticsearch event storage, Micrometer/Prometheus metrics.
- `user-service`: signup/login, BCrypt password hashing, JWT issuance, MongoDB persistence.
- `order-service`: order CRUD APIs backed by MongoDB.
- `shared-common`: placeholder module retained from the original project; it is not required by the current Maven reactor.

## Technology baseline

- Java 8
- Spring Boot 2.7.18
- Spring Cloud 2021.0.9
- Spring Data Elasticsearch 4.4.x with Elasticsearch 7.17.15
- Redis 7, Kafka (single-node KRaft), RabbitMQ 4, MongoDB 7
- Prometheus 3.5.0 and Grafana 11.6.0

## Run the complete stack

From this directory (`Application`), on Windows PowerShell:

```powershell
Copy-Item .env.example .env
# Edit .env and replace all placeholder secrets before starting the stack.
docker compose down --remove-orphans
docker compose config
docker compose up --build -d
docker compose ps
```

`docker compose down --remove-orphans` removes old containers from an earlier Compose layout but keeps named data volumes. Do not add `-v` unless you intentionally want to delete MongoDB, RabbitMQ, Elasticsearch, Prometheus, and Grafana data. The first build needs internet access to download Maven dependencies and container images. The Dockerfiles build each Maven module from the root aggregator so the relative parent POM resolves correctly.

## Local URLs

| Component | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| User Service (direct) | http://localhost:8081 |
| User Service replica (direct) | http://localhost:8083 |
| Order Service (direct) | http://localhost:8082 |
| Gateway Swagger UI | http://localhost:8080/swagger-ui.html |
| Gateway Actuator health | http://localhost:8080/actuator/health |
| Prometheus metrics | http://localhost:8080/actuator/prometheus |
| Prometheus UI | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Elasticsearch | http://localhost:9200 |
| RabbitMQ management | http://localhost:15672 |
| Kafka host listener | localhost:9092 |

## Environment variables

For local development, the compose file provides development defaults. Before using outside a local machine, set strong values for:

- `JWT_SECRET` — at least 32 bytes; must be identical for User Service and Gateway.
- `RABBITMQ_PASSWORD` — RabbitMQ password passed to both broker and Gateway.
- `GRAFANA_ADMIN_PASSWORD` — Grafana `admin` password.

A `.env` file is loaded automatically by Docker Compose. Do not commit real secrets. If you already have a persistent Grafana volume, its existing admin password remains in effect; `GF_SECURITY_ADMIN_PASSWORD` only initializes a new Grafana data directory.

## API flow

1. Create a user through `POST /user/signup`.
2. Login through `POST /user/login`; the response contains a JWT.
3. Send protected requests through the Gateway with `Authorization: Bearer <token>`.
4. Gateway forwards requests to User/Order services. User Service requests are distributed over two container instances.
5. Gateway request events are handed off to a bounded async executor, published to Kafka, consumed, deduplicated using an in-memory Bloom filter plus an Elasticsearch existence check, and indexed in `gateway-logs`.
6. 5xx events are also sent asynchronously to the `gateway-alerts` RabbitMQ queue and logged by its listener.
7. Prometheus scrapes `/actuator/prometheus`; Grafana provisions a starter dashboard automatically.

## Important limitations

This is a local-development demonstration, not a production deployment. Kafka and Elasticsearch are single-node, RabbitMQ has local-development credentials by default, the Bloom filter is in-memory and resets on restart, and the default JWT secret must be replaced. Add TLS, secret management, persistent Kafka storage, authentication/authorization for infrastructure endpoints, and multi-node replication before production use.

## Build locally

With Maven 3.8+ and JDK 8 installed:

```bash
mvn -B -DskipTests clean package
```

The source archive supplied for this repair did not contain a Maven wrapper. Ensure Maven is installed locally before running the command.


## Included completion assets

- `scripts/smoke-test.ps1` performs the final HTTP/API/event-index smoke test after the stack is built and running.
- `docs/testing-checklist.md` lists the full acceptance sequence, including rate limiting, retry, circuit breaker, load balancing, RabbitMQ alerting, Kafka indexing, and Grafana.
- `docs/architecture.md` contains the architecture diagram.

Run the smoke-test script only from the extracted `Application` root folder. It is intentionally not run during source preparation.

## Unit tests

JUnit/Mockito unit tests cover core User Service authentication flows, Order Service behavior, and Bloom-filter membership. They are included in the source tree and should be run during the testing phase with `mvn test`.
