# Local integration test checklist

Run these tests from the `Application` directory after creating `.env` and starting the stack.

## 1. Container startup

```powershell
docker compose ps
docker compose logs --tail=100 api-gateway user-service user-service-replica order-service
```

Expected: all services remain running; Gateway logs show a successful Spring Boot startup. MongoDB, Redis, Kafka, RabbitMQ, and Elasticsearch should be reachable from the Gateway container.

## 2. Automated smoke test

```powershell
./scripts/smoke-test.ps1
```

The script creates a throwaway test account unless `TEST_USER_EMAIL` and `TEST_USER_PASSWORD` are set. It checks health, the Prometheus endpoint, signup/login, the authenticated profile, Order create/read/delete, and waits for the Gateway event to appear in Elasticsearch.

## 3. Redis rate limiting

Use a valid Bearer token and make more than 20 rapid requests from the same client IP to a routed endpoint, for example `GET http://localhost:8080/orders`. The initial burst is 20 tokens and refill rate is 10 tokens/second. Requests over the available token balance should receive HTTP 429. Wait a few seconds before normal testing resumes.

## 4. Load balancing

Make repeated authenticated `GET http://localhost:8080/user` requests, then inspect both service logs:

```powershell
docker compose logs --tail=200 user-service user-service-replica
```

Both instances should receive requests over repeated calls. The simple discovery client has two static endpoints; it is not an Eureka/Consul registry and does not perform active instance registration.

## 5. Kafka and Elasticsearch

After the smoke test, query Elasticsearch:

```powershell
Invoke-RestMethod 'http://localhost:9200/gateway-logs/_search?pretty'
```

The `gateway-logs` index should contain events with `correlationId`, `serviceName`, `method`, `path`, `statusCode`, `latencyMs`, and `timestamp`. Check Gateway logs for `Event published` and `Event successfully indexed`.

## 6. RabbitMQ alert path

Temporarily stop Order Service:

```powershell
docker compose stop order-service
```

Call an authenticated `GET http://localhost:8080/orders` request. The Gateway should eventually return a 503 fallback response after the retry/circuit-breaker path. Then inspect:

```powershell
docker compose logs --tail=200 api-gateway
```

Look for `Gateway alert queued` and `Gateway alert received from RabbitMQ`. Restore the service:

```powershell
docker compose start order-service
```

## 7. Prometheus and Grafana

- Prometheus UI: `http://localhost:9090/targets` — `api-gateway` target should be UP.
- In Prometheus, query `gateway_requests_total` and `gateway_request_duration_seconds_count`.
- Grafana: `http://localhost:3000` — sign in with username `admin` and the `GRAFANA_ADMIN_PASSWORD` from `.env`; the provisioned `API Gateway Overview` dashboard should be available in the `API Gateway` folder.

## Notes

These are local demonstration checks, not production certification. Single-node Kafka/Elasticsearch, development secrets, a memory-only Bloom filter, and IP-based limiting are intentional demo constraints. The smoke test must be run against the actual Docker environment; static file validation alone does not prove runtime behavior.
