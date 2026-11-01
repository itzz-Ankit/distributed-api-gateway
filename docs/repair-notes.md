# Repair Notes and Validation Status

This ZIP is an amended copy of the uploaded project. The original uploaded ZIP was left unchanged.

## Source/configuration corrections applied

- Removed a duplicate `GatewayExceptionHandler` public class that existed in both `GatewayExceptionHandler.java` and the misspelled `GloablExceptionHandler.java`; retained the fuller handler in the correctly named file.
- Corrected the Gateway Bulkhead configuration to use Spring Cloud's reactive `ReactiveResilience4jBulkheadProvider` API.
- Added the JJWT runtime implementation and Jackson modules to the Gateway; aligned the Gateway and User Service development JWT secret defaults and Docker environment variable.
- Kept the User Service matcher API on `antMatchers` because the Spring Boot 2.7.18 BOM manages Spring Security 5.7.11; the string-varargs `requestMatchers` method is a Spring Security 5.8 API. Removed an invalid unused JWT import.
- Moved Gateway event/Kafka classes into package-matching source folders. Removed misplaced Compose/Prometheus YAML files from Java source folders.
- Connected the Correlation ID filter to the existing in-memory observability service, so the observability endpoint receives request records. Separated its Micrometer metric names from the Gateway request-filter metric names to avoid duplicate Prometheus family labels.
- Changed the User Service profile endpoint to return the authenticated user's response DTO rather than a hard-coded mock object containing a password field.
- Added explicit Kafka topic declaration and first-write Elasticsearch index creation for the Gateway event consumer.
- Corrected Order Service missing-record handling to return 404 and replaced its broad RuntimeException-to-404 handler with status-accurate exception handling.
- Added MongoDB settings for Order Service and corrected Docker port/environment handling for both services.
- Reworked the three Dockerfiles to build from the root Maven aggregator; corrected Compose build contexts, service DNS names, published ports, Prometheus mount path, Gateway Docker profile, and infrastructure readiness checks.
- Added Grafana Prometheus datasource/dashboard provisioning, a starter overview dashboard, a README, architecture diagram, a PowerShell smoke-test script, and a manual integration checklist.

## Static validations performed in this workspace

- All Maven POM files parse as XML.
- All YAML files parse, with duplicate-key detection enabled.
- Grafana dashboard JSON parses.
- No duplicate public Java class declarations were found.
- Java package declarations match their source folder paths after the event-class moves.

## Validations not performed here

The execution environment used to prepare this ZIP does not have Maven or Docker installed, and external DNS/network access to Maven Central is unavailable. Therefore a Maven compile, Docker image build, container health check, and end-to-end runtime test could not be run here. Run the commands below on the development machine before claiming the project is fully tested:

```bash
mvn -B clean package
docker compose config
docker compose up --build -d
docker compose ps
```

Then set `TEST_USER_EMAIL` and `TEST_USER_PASSWORD` and run `./scripts/smoke-test.ps1` from PowerShell. Also test service-failure fallback, rate-limit 429 responses, Kafka event indexing in `gateway-logs`, RabbitMQ 5xx alerts, and Grafana panels.
