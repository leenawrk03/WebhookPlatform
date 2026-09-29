# Webhook Delivery Platform — Phase 1

Centralized webhook receiver and delivery platform.
External services (Stripe, GitHub, Razorpay, Shopify, GitLab, Jira, Slack, Discord, Twilio, Jenkins)
post events to this API; the API validates and stores them in PostgreSQL, publishes them to Kafka,
and a delivery worker forwards them to the destinations users registered. Redis holds retry state
with exponential backoff. Prometheus scrapes metrics and Grafana displays them.

## Stack

| Layer        | Technology                                  |
|--------------|---------------------------------------------|
| Frontend     | Angular 18 (standalone), TypeScript, RxJS, Chart.js |
| Backend      | Java 21, Spring Boot 3.3, Spring Security (JWT) |
| Database     | PostgreSQL 16 + JPA + Flyway                |
| Messaging    | Apache Kafka (KRaft)                        |
| Retry state  | Redis 7 (sorted set with due timestamps)    |
| Monitoring   | Micrometer + Prometheus + Grafana           |
| Testing      | JUnit 5, Mockito, WireMock                  |
| DevOps       | Docker + Docker Compose                     |

## Run everything

```bash
docker compose up --build
```

| Service    | URL                            |
|------------|--------------------------------|
| Frontend   | http://localhost:4200          |
| Backend    | http://localhost:8080          |
| Prometheus | http://localhost:9090          |
| Grafana    | http://localhost:3000 (admin/admin) |
| Metrics    | http://localhost:8080/actuator/prometheus |

Default login: `admin` / `admin123` (override with `ADMIN_USERNAME` / `ADMIN_PASSWORD`).

## Run locally without Docker

```bash
# infrastructure only
docker compose up postgres redis kafka

# backend
cd backend && ./mvnw spring-boot:run     # or: mvn spring-boot:run

# frontend (proxies /api to localhost:8080)
cd frontend && npm install && npm start
```

## Try it end to end

1. Sign in at http://localhost:4200.
2. Add a destination — e.g. name `Stripe Payments`, source `stripe`, URL `https://webhook.site/<your-id>`.
3. Send a test webhook:

```bash
curl -X POST http://localhost:8080/api/webhooks/stripe \
  -H 'Content-Type: application/json' \
  -d '{"id":"evt_123","type":"payment_intent.succeeded","data":{"amount":4200}}'
```

4. The event appears under **Events**; the delivery attempt appears on the dashboard.
   If the destination is down, Redis schedules a retry (10s, 20s, 40s, 80s, …, 5 attempts max).

## API

| Method | Path                      | Auth   | Description                        |
|--------|---------------------------|--------|------------------------------------|
| POST   | `/api/auth/login`         | public | Returns a JWT                      |
| POST   | `/api/webhooks/{source}`  | public | Inbound webhook from a platform    |
| GET    | `/api/events`             | JWT    | Paged event list (`?source=&page=&size=`) |
| GET    | `/api/events/{id}`        | JWT    | Event detail + delivery attempts   |
| GET    | `/api/deliveries`         | JWT    | Recent delivery attempts           |
| GET    | `/api/stats`              | JWT    | Dashboard counters                 |
| GET    | `/api/destinations`       | JWT    | List destinations                  |
| POST   | `/api/destinations`       | JWT    | Create destination                 |
| PUT    | `/api/destinations/{id}`  | JWT    | Update destination                 |
| DELETE | `/api/destinations/{id}`  | JWT    | Delete destination                 |

Outgoing deliveries carry `X-Webhook-Source`, `X-Webhook-Event-Id`, `X-Webhook-Event-Type`,
`X-Webhook-Attempt`, and `X-Webhook-Signature` (HMAC-SHA256 of the payload) when the destination
has a secret.

## Metrics exposed

`webhook_events_received_total`, `webhook_events_duplicate_total`,
`webhook_deliveries_success_total`, `webhook_deliveries_failed_total`,
`webhook_delivery_retries_total`, `webhook_delivery_duration_seconds`,
plus the standard `http_server_requests_seconds` endpoint latency.
A ready-made Grafana dashboard is provisioned under folder **Webhook Platform**.

## Tests

```bash
cd backend && mvn test
```

- `WebhookIngestServiceTest` — validation, idempotency, Kafka publish (JUnit + Mockito)
- `DeliveryServiceWireMockTest` — success and retry paths against a WireMock destination

## Layout

```
webhook-platform/
├── backend/          Spring Boot API + Kafka worker + retry scheduler
├── frontend/         Angular dashboard
├── monitoring/       Prometheus config + Grafana provisioning
├── postman/          Postman collection
└── docker-compose.yml
```

## Phase 1 checklist

Backend: REST APIs ✔ · PostgreSQL + JPA ✔ · webhook validation ✔ · destination CRUD ✔ ·
Kafka producer/consumer ✔ · delivery worker ✔ · delivery status tracking ✔
Frontend: login ✔ · dashboard ✔ · event list/detail ✔ · destination management ✔ · delivery status ✔
