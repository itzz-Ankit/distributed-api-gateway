# Distributed API Gateway & Observability Platform

A multi-module, microservices-based backend platform built with **Java 8, Spring Boot, and Spring Cloud Gateway**. The project demonstrates how an API Gateway can act as a single entry point for multiple backend services while providing authentication, traffic control, resilience, asynchronous event processing, and operational visibility.

The platform places dedicated **User** and **Order** services behind an API Gateway. Incoming requests can be authenticated with JWTs, routed to the appropriate service, protected by Redis-backed rate limiting, and distributed across service instances. The gateway also emits request telemetry for asynchronous processing through Kafka, stores processed events in Elasticsearch, and exposes application metrics for Prometheus and Grafana.

> **Project scope:** This repository is an educational/local-development implementation of distributed backend patterns. It is not a production-ready deployment. See [Limitations and production considerations](#limitations-and-production-considerations).

---

## Table of contents

- [Architecture at a glance](#architecture-at-a-glance)
- [Key capabilities](#key-capabilities)
- [Repository structure](#repository-structure)
- [Technology stack](#technology-stack)
- [Request lifecycle](#request-lifecycle)
- [Services and endpoints](#services-and-endpoints)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Run the platform with Docker Compose](#run-the-platform-with-docker-compose)
- [Run services locally from an IDE](#run-services-locally-from-an-ide)
- [Service and infrastructure URLs](#service-and-infrastructure-urls)
- [Build and test](#build-and-test)
- [Observability and event processing](#observability-and-event-processing)
- [Troubleshooting](#troubleshooting)
- [Limitations and production considerations](#limitations-and-production-considerations)
- [Project assets](#project-assets)

## Architecture at a glance

The API Gateway is the public entry point for client requests. It applies gateway-level concerns before forwarding traffic to backend services.

```text
                         +----------------------+
                         |        Client        |
                         +----------+-----------+
                                    |
                                    v
                     +--------------+---------------+
                     |          API Gateway         |
                     |       Spring Cloud Gateway  |
                     |------------------------------|
                     | JWT validation               |
                     | Request routing              |
                     | Redis rate limiting          |
                     | Load balancing               |
                     | Resilience / Bulkhead        |
                     | Correlation ID and metrics   |
                     +------+----------------+------+
                            |                |
                   +--------+----+     +-----+---------+
                   | User Service|     | Order Service |
                   | 8081 / 8083 |     |     8082      |
                   +------+------+     +------+--------+
                          |                   |
                          +---------+---------+
                                    |
                              +-----+-----+
                              |  MongoDB  |
                              +-----------+

       Gateway telemetry / events
                    |
                    v
          +---------+----------+
          | Async event handoff|
          +---------+----------+
                    |
                    v
                +---+---+
                | Kafka |
                +---+---+
                    |
                    v
            +-------+---------+
            | Event consumer  |
            +---+----------+--+
                |          |
                v          v
        +-------+---+  +---+----------------+
        |Elasticsearch|  | RabbitMQ alerts |
        | gateway-logs|  | gateway-alerts  |
        +-------------+  +------------------+

       Metrics endpoint --> Prometheus --> Grafana
```

The diagram is a logical view of the intended request and telemetry flow. Exact runtime behavior depends on the active configuration and which optional infrastructure containers are running.

## Key capabilities

| Capability | What it does |
|---|---|
| Centralized API entry point | Routes client traffic through a single Spring Cloud Gateway endpoint. |
| JWT-based authentication | Validates bearer tokens at the gateway; the User Service handles signup, login, password hashing, and token issuance. |
| Redis rate limiting | Applies a configurable request quota at the gateway using Redis. |
| Service load balancing | Supports distributing User Service traffic across multiple instances. |
| Resilience patterns | Includes retry/circuit-breaker configuration and a custom semaphore-based bulkhead filter to help isolate upstream failures and concurrency. |
| Correlation IDs | Carries a request correlation identifier through gateway processing to support tracing and log investigation. |
| Asynchronous telemetry | Hands gateway request events to an asynchronous processing path and publishes them to Kafka. |
| Event persistence | Consumes Kafka events and indexes them in Elasticsearch, using deduplication checks. |
| Failure alert queue | Routes server-error events to a RabbitMQ queue for asynchronous alert handling. |
| Metrics and dashboards | Exposes application metrics through Micrometer/Actuator for Prometheus scraping and Grafana visualization. |
| User and Order APIs | Provides user authentication flows and order CRUD operations in separate services. |

## Repository structure

The repository uses a **Maven multi-module layout**. Each runnable service has its own `pom.xml`, Dockerfile, application source, configuration, and tests. Shared Docker Compose, Prometheus, documentation, and Grafana provisioning files are maintained at the repository root or under `infrastructure/`.

```text
distributed-api-gateway/
├── api-gateway/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── Dto/
│   │   │   │   │   ├── request.java
│   │   │   │   │   └── response.java
│   │   │   │   └── com/platform/gateway/
│   │   │   │       ├── ApiGatewayApplication.java
│   │   │   │       ├── BulkheadConfiguration.java
│   │   │   │       ├── CircuitBreakerEventLogger.java
│   │   │   │       ├── CorrelationIdGlobalFilter.java
│   │   │   │       ├── FallbackController.java
│   │   │   │       ├── GatewayAsyncConfiguration.java
│   │   │   │       ├── GatewayConfig.java
│   │   │   │       ├── GatewayExceptionHandler.java
│   │   │   │       ├── GatewayMetricsFilter.java
│   │   │   │       ├── GatewayObservabilityController.java
│   │   │   │       ├── GatewayRequestEvent.java
│   │   │   │       ├── JwtAuthenticationManager.java
│   │   │   │       ├── JwtService.java
│   │   │   │       ├── RateLimitConfig.java
│   │   │   │       ├── SecurityConfig.java
│   │   │   │       ├── Service.java
│   │   │   │       ├── event/
│   │   │   │       │   ├── Consumer.java
│   │   │   │       │   ├── GatewayRabbitIntegration.java
│   │   │   │       │   ├── KafkaTopicConfiguration.java
│   │   │   │       │   └── Producer.java
│   │   │   │       └── observability/
│   │   │   │           └── GatewayEventBloomFilter.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-docker.yml
│   │   └── test/java/com/platform/gateway/observability/
│   │       └── GatewayEventBloomFilterTest.java
│   ├── Dockerfile
│   ├── .dockerignore
│   └── pom.xml
│
├── user-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── Config/
│   │   │   │   │   ├── PasswordConfig.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   ├── Exception/
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── Mapper/
│   │   │   │   │   └── UserMapper.java
│   │   │   │   ├── Security/
│   │   │   │   │   └── JwtAuthFilter.java
│   │   │   │   ├── Util/
│   │   │   │   │   └── JwtUtil.java
│   │   │   │   ├── com/platform/userservice/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── UserServiceApplication.java
│   │   │   │   ├── dto_request/
│   │   │   │   │   ├── LoginReq.java
│   │   │   │   │   └── SignupReq.java
│   │   │   │   └── dto_response/
│   │   │   │       ├── AuthResponse.java
│   │   │   │       └── UserResponse.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/java/com/platform/userservice/
│   │       └── UserServiceTest.java
│   ├── Dockerfile
│   └── pom.xml
│
├── order-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── Dto_req/
│   │   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   │   └── UpdateOrderRequest.java
│   │   │   │   ├── Dto_response/
│   │   │   │   │   └── OrderResponse.java
│   │   │   │   ├── Event/
│   │   │   │   │   └── OrderCreatedEvent.java
│   │   │   │   ├── Mapper/
│   │   │   │   │   └── OrderMapper.java
│   │   │   │   └── com/platform/orderservice/
│   │   │   │       ├── GlobalExceptionalHandler.java
│   │   │   │       ├── Order.java
│   │   │   │       ├── OrderController.java
│   │   │   │       ├── OrderRepository.java
│   │   │   │       ├── OrderService.java
│   │   │   │       └── OrderServiceApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/java/com/platform/orderservice/
│   │       └── OrderServiceTest.java
│   ├── Dockerfile
│   └── pom.xml
│
├── shared-common/
│   └── pom.xml
│
├── docs/
│   ├── architecture.md
│   ├── implementation-inventory.md
│   ├── repair-notes.md
│   └── testing-checklist.md
│
├── infrastructure/
│   └── grafana/
│       ├── dashboards/
│       │   └── api-gateway-overview.json
│       └── provisioning/
│           ├── dashboards/
│           │   └── dashboards.yml
│           └── datasources/
│               └── prometheus.yml
│
├── scripts/
│   └── smoke-test.ps1
│
├── .dockerignore
├── .env.example
├── .gitignore
├── docker-compose.yml
├── pom.xml
├── prometheus.yml
└── README.md
```

> **Note:** This tree reflects the files currently committed to this repository. Package-folder capitalization (for example, `Dto`, `Config`, and `dto_request`) is shown as it exists in the source tree. The `shared-common` module currently contains only its Maven descriptor; it does not currently contain shared Java source files.

### Module responsibilities

- **`api-gateway`** — gateway routing, JWT validation, Redis rate limiting, load balancing, resilience filters/configuration, correlation IDs, Kafka event production/consumption integration, RabbitMQ alert handling, Elasticsearch event storage, and Actuator/Micrometer metrics.
- **`user-service`** — user signup and login, BCrypt password hashing, JWT generation, and MongoDB persistence.
- **`order-service`** — order creation, retrieval, update, and deletion APIs backed by MongoDB.
- **`shared-common`** — retained from the original project structure as a placeholder. It is not currently required by the active Maven reactor.

## Technology stack

| Area | Technology / version |
|---|---|
| Language | Java 8 |
| Application framework | Spring Boot 2.7.18 |
| Cloud components | Spring Cloud 2021.0.9 |
| API gateway | Spring Cloud Gateway 3.1.9 |
| Persistence | Spring Data MongoDB with MongoDB 7 |
| Authentication | Spring Security, JWT, BCrypt |
| Rate limiting | Redis 7 |
| Resilience | Resilience4j configuration and custom semaphore-based bulkhead filter |
| Messaging | Apache Kafka (single-node KRaft), RabbitMQ 4 |
| Event storage | Spring Data Elasticsearch 4.4.x, Elasticsearch 7.17.15 |
| Metrics | Spring Boot Actuator, Micrometer, Prometheus 3.5.0 |
| Dashboards | Grafana 11.6.0 |
| Build | Maven 3.8+ |
| Local infrastructure | Docker Compose |

## Request lifecycle

A typical authenticated request follows this sequence:

1. **Sign up:** a client registers using `POST /user/signup`.
2. **Authenticate:** the client calls `POST /user/login`. The User Service verifies the credentials and returns a JWT.
3. **Send a protected request:** the client calls the gateway and supplies the token in the `Authorization: Bearer <token>` header.
4. **Validate and control traffic:** the gateway validates the token and applies configured gateway filters, including rate limiting and resilience controls.
5. **Route to a backend:** the gateway forwards the request to the User or Order service. User Service traffic can be distributed across two configured instances.
6. **Process the business operation:** the backend service executes the operation and uses MongoDB for persistence where applicable.
7. **Publish request telemetry:** gateway request events are handed to a bounded asynchronous executor and published to Kafka.
8. **Consume and store events:** the consumer processes events, performs deduplication using an in-memory Bloom filter together with an Elasticsearch existence check, and indexes records in the `gateway-logs` index.
9. **Handle server-error events:** 5xx events are also sent to the `gateway-alerts` RabbitMQ queue for asynchronous listener processing.
10. **Observe the platform:** Prometheus scrapes the gateway's Actuator metrics endpoint, and Grafana provides a dashboard view of the collected metrics.

## Services and endpoints

The following are the local development ports used by the project configuration.

| Component | Local address | Purpose |
|---|---|---|
| API Gateway | `http://localhost:8080` | Client-facing gateway |
| User Service | `http://localhost:8081` | User API instance |
| User Service replica | `http://localhost:8083` | Additional User API instance |
| Order Service | `http://localhost:8082` | Order API |
| Gateway Swagger UI | `http://localhost:8080/swagger-ui.html` | API documentation UI |
| Gateway health | `http://localhost:8080/actuator/health` | Gateway health indicator |
| Gateway Prometheus metrics | `http://localhost:8080/actuator/prometheus` | Metrics scrape endpoint |
| Prometheus UI | `http://localhost:9090` | Metrics query interface |
| Grafana | `http://localhost:3000` | Dashboard UI |
| Elasticsearch | `http://localhost:9200` | Indexed event store |
| RabbitMQ management | `http://localhost:15672` | Broker management UI |
| Kafka host listener | `localhost:9092` | Kafka bootstrap listener |

These addresses are intended for local development. They are not public deployment URLs.

## Prerequisites

Before starting the full stack, install:

- **JDK 8** compatible with the configured project baseline.
- **Maven 3.8 or later**.
- **Docker Desktop** with Docker Compose support.
- **Git** to clone the repository.

The first Docker build requires internet access to download Maven dependencies and container images. The Dockerfiles build modules from the root Maven aggregator so that the parent POM can be resolved correctly.

## Configuration

### Environment variables

A local `.env` file is read automatically by Docker Compose. Start from the example file:

```powershell
Copy-Item .env.example .env
```

Open `.env` and replace placeholder values before starting the stack. At a minimum, configure:

| Variable | Purpose | Guidance |
|---|---|---|
| `JWT_SECRET` | Signs and validates JWTs in the User Service and Gateway | Use a strong secret of at least 32 bytes. The same value must be configured in both services. |
| `RABBITMQ_PASSWORD` | Password for the RabbitMQ broker and gateway connection | Replace the local development value with a strong password. |
| `GRAFANA_ADMIN_PASSWORD` | Initial Grafana administrator password | Choose a unique password and keep it private. |

**Never commit a real `.env` file, passwords, API keys, signing secrets, or production credentials.** Commit only `.env.example` with safe placeholders.

If a Grafana data volume already exists, Grafana may continue using the administrator password stored in that volume. `GF_SECURITY_ADMIN_PASSWORD` initializes credentials for a new Grafana data directory; changing the environment variable does not necessarily reset an existing volume's password.

## Run the platform with Docker Compose

Run these commands from the repository root (the directory containing `docker-compose.yml`).

### 1. Create and edit the environment file

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace all placeholder secrets.

### 2. Validate the Compose configuration

```powershell
docker compose config
```

Resolve any configuration errors before continuing.

### 3. Build and start the containers

```powershell
docker compose up --build -d
```

### 4. Check container status

```powershell
docker compose ps
```

Allow health checks and first-time initialization to complete. You can inspect a service's logs with:

```powershell
docker compose logs -f api-gateway
```

Replace `api-gateway` with the relevant Compose service name when inspecting another component.

### Stop the stack

```powershell
docker compose down
```

The command above stops and removes the Compose containers and network while retaining named data volumes. To remove stored MongoDB, RabbitMQ, Elasticsearch, Prometheus, and Grafana data as well, Docker Compose supports the `-v` option:

```powershell
docker compose down -v
```

**Use `-v` only when you intentionally want to delete the persisted development data.**

When migrating from an older Compose layout, `docker compose down --remove-orphans` can also remove orphaned containers while retaining named volumes.

## Run services locally from an IDE

The backend services can also be launched individually from an IDE such as IntelliJ IDEA, while infrastructure dependencies run separately through Docker Compose.

Typical local service ports:

- API Gateway: `8080`
- User Service: `8081`
- Second User Service instance: `8083`
- Order Service: `8082`

To run a second User Service instance, create a second run configuration for the same application and override its server port to `8083` (for example, with `--server.port=8083`). Ensure that the gateway's service discovery/load-balancing configuration points to the instances you intend to use.

The exact IDE run configuration and environment-variable setup may differ between local machines. Make sure required MongoDB, Redis, Kafka, RabbitMQ, and Elasticsearch dependencies are reachable before starting features that rely on them.

## Build and test

### Package the Maven modules

From the repository root:

```bash
mvn -B -DskipTests clean package
```

This command compiles and packages the modules while skipping test execution.

### Run unit tests

```bash
mvn test
```

JUnit and Mockito tests in the source tree cover core User Service authentication flows, Order Service behavior, and Bloom-filter membership. Test results depend on the current source and configuration; run the command locally to verify the current checkout.

The supplied source archive does not include a Maven wrapper. Install Maven locally and ensure that `mvn` is available on your PATH before running these commands.

### Run the smoke-test script

After the stack is running and its dependencies are ready, execute the PowerShell smoke test from the repository root:

```powershell
.\scripts\smoke-test.ps1
```

The script performs HTTP/API and event-index checks against the running stack. It is intended to be run after startup, not during source preparation.

## Observability and event processing

### Prometheus metrics

The gateway exposes metrics through Spring Boot Actuator at:

`http://localhost:8080/actuator/prometheus`

Prometheus is configured to scrape the gateway metrics endpoint. Open `http://localhost:9090` to query collected time-series data.

### Grafana dashboards

Grafana is available at `http://localhost:3000`. The project provisions a starter dashboard through its local configuration. Use it to explore the metrics collected by Prometheus.

### Kafka event pipeline

Gateway telemetry is passed to an asynchronous executor, published to Kafka, and consumed by the event-processing component. Successfully processed events are indexed in Elasticsearch under the `gateway-logs` index. The in-memory Bloom filter is used as an initial deduplication check, with an Elasticsearch existence check as an additional safeguard.

### RabbitMQ alert queue

Server-error (5xx) events are published asynchronously to the `gateway-alerts` queue. The configured listener receives and logs these alert messages. This queue is intended as a demonstration of asynchronous alert handoff, not as a complete incident-management system.

## Troubleshooting

| Symptom | Checks |
|---|---|
| A service does not start | Check `docker compose ps` and `docker compose logs -f <service-name>`; verify its port is available. |
| Gateway cannot reach a backend | Confirm the backend health endpoint, gateway route configuration, service instance ports, and container network settings. |
| MongoDB or another dependency is unavailable | Confirm the relevant container is running and healthy, then verify the connection settings in the environment/configuration. |
| JWT validation fails | Check that the User Service and Gateway use the same `JWT_SECRET`, that the token has not expired, and that the request includes the `Bearer` prefix. |
| Rate limiting behaves unexpectedly | Confirm Redis is reachable and review the gateway rate-limit configuration and client identity/key-resolution strategy. |
| Metrics are missing | Check the gateway Actuator endpoint, Prometheus target status, and the scrape configuration in `prometheus.yml`. |
| Events are not visible in Elasticsearch | Inspect Kafka broker/consumer logs, Elasticsearch health, index availability, and event-consumer logs. |
| Grafana login does not accept the current environment password | Check whether an existing Grafana data volume retains an earlier administrator credential. |

For diagnosis, inspect the logs of the affected component rather than restarting or deleting all containers and volumes immediately.

## Limitations and production considerations

This repository is designed as a local-development and learning platform. The following considerations should be addressed before production use:

- Kafka and Elasticsearch are configured as single-node components; they do not provide production-grade high availability in this setup.
- RabbitMQ uses local-development credentials by default unless overridden through environment variables.
- The Bloom filter is in-memory and is reset when the application restarts.
- The default JWT secret must be replaced, and secrets should be managed through a dedicated secret-management mechanism.
- Infrastructure management endpoints and dashboards should be protected with appropriate authentication and network controls.
- Production deployments should use TLS, durable storage, backups, monitoring/alerting policies, access control, and multi-node replication where required.
- Capacity limits, retry policies, circuit-breaker thresholds, retention policies, and failure behavior should be load-tested and tuned for the target environment.

## Project assets

- **`docs/architecture.md`** — architecture notes and diagram.
- **`docs/testing-checklist.md`** — acceptance checklist covering rate limiting, retry, circuit breaker, load balancing, RabbitMQ alerting, Kafka indexing, and Grafana.
- **`scripts/smoke-test.ps1`** — PowerShell smoke-test script for the running local stack.

---

## Purpose

The goal of this project is to demonstrate how common distributed-backend concerns can be brought together around an API Gateway: securing requests, controlling traffic, isolating failures, processing telemetry asynchronously, persisting operational events, and making service behavior observable.
