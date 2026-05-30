# Smart URL Intelligence Hub — Project Plan

> **Intended audience:** AI coding agents executing this plan stage by stage.
>
> **Before starting each stage:** ask the user at least the following:
> 1. Are there any requirements or constraints for this stage not covered in the plan?
> 2. Should any decisions from previous stages be revisited before proceeding?
> 3. Are there any open questions in Section 7 relevant to this stage that need resolving?
>
> Do not implement beyond what a stage specifies — keep changes scoped.

---

## 1. Project Summary

Smart URL Intelligence Hub is a microservice-based platform where users shorten URLs with
optional custom slugs and expiration policies. Every click on a short link emits an event
carrying geo, device, and referrer metadata. An analytics service consumes these events,
aggregates statistics, and when a link crosses a configured click threshold it publishes a
trigger event. A webhook service reacts to that trigger and fires an HTTP callback to a
user-defined URL.

The core engineering value is in the event pipeline, not CRUD: the Outbox pattern
guarantees no click event is lost, Redis counters handle high-frequency increments,
threshold detection drives async webhook delivery, and a Circuit Breaker protects against
flaky webhook targets.

---

## 2. Architecture

### 2.1 Module layout

```
smart-url-hub/
├── common-lib/              <- Shared Spring Boot starter (security, error handling, observability)
├── config-server/           <- Spring Cloud Config Server (central config & secrets)
├── auth-service/            <- Issues & revokes JWT tokens
├── api-gateway/             <- Single ingress, JWT validation, header injection
├── link-service/            <- Link CRUD, redirect, Outbox publisher
├── analytics-service/       <- Click event consumer, stats, threshold detection
├── webhook-service/         <- Threshold consumer, HTTP delivery, Circuit Breaker
├── config/                  <- Config Server filesystem backend
│   ├── application.yml      <- Shared config for all services
│   ├── auth-service.yml
│   ├── api-gateway.yml
│   ├── link-service.yml
│   ├── analytics-service.yml
│   └── webhook-service.yml
└── docker/
    └── docker-compose.yml
```

### 2.2 Request flow

```
Client
  |
  v
API Gateway  (validates JWT locally, checks Redis revocation set,
  |           injects X-User-Id / X-User-Email / X-User-Roles headers)
  |
  +---> Auth Service       (register, login, token issuance)
  +---> Link Service       (link CRUD + public redirect)
  +---> Analytics Service  (read-only stats API)
  +---> Webhook Service    (webhook config CRUD)

Link Service
  |-- [Outbox table] --@Scheduled poller--> RabbitMQ: click.events
                                                  |
                                            Analytics Service
                                            (enrich, store, count)
                                            [Feign -> Webhook Service: thresholds]
                                                  |-- threshold crossed
                                                  v
                                            RabbitMQ: threshold.reached
                                                  |
                                            Webhook Service
                                            [Feign -> Link Service: ownership check]
                                            (Circuit Breaker HTTP delivery)
```

### 2.3 Config Server flow

All services fetch configuration from config-server on startup. Secrets (DB credentials,
RabbitMQ credentials, Redis URI, JWT key paths) live only in `config/` files — never
hardcoded in service source. Services fail fast if config-server is unreachable.

Config server uses a native filesystem backend (`config/` directory). Each service
`application.yml` contains only:
```yaml
spring:
  config:
    import: "configserver:http://config-server:8888"
  application:
    name: {service-name}
```

**Future (non-local environments):** for staging/prod the config-server backend
will switch from `native` filesystem to **HashiCorp Vault**. Secrets (DB
credentials, RabbitMQ creds, Redis password, JWT keys) will live in Vault and
the config-server will resolve them via the Spring Cloud Config Vault
integration. Downstream services keep declaring only
`spring.config.import=configserver:...` and never know the storage backend
changed.

### 2.4 Network security

Only `api-gateway` is on both the host-facing and internal Docker networks. All other
services — including `config-server` — are on `hub-internal` only. Downstream services
trust `X-User-*` headers unconditionally because no external traffic can reach them.

### 2.5 RabbitMQ topology

```
Exchange: click.events      (topic, durable)
  +-- Queue: analytics.click.queue  -> Analytics Service
      +-- DLQ: analytics.click.dlq

Exchange: threshold.reached (topic, durable)
  +-- Queue: webhook.trigger.queue  -> Webhook Service
      +-- DLQ: webhook.trigger.dlq
```

### 2.6 Observability

Every service exposes structured JSON logs (Logback + logstash-logback-encoder) with
`traceId` and `spanId` auto-injected via Micrometer Tracing MDC. Traces are shipped to
Zipkin (Brave bridge, B3 propagation). Metrics are exposed at `/actuator/prometheus`
and scraped by Prometheus.

B3 trace headers must be propagated across RabbitMQ message boundaries via a
`MessagePostProcessor` on both producer and consumer sides.

Required custom metrics:

| Metric                         | Type    | Service           | Tags                                    |
|--------------------------------|---------|-------------------|-----------------------------------------|
| `link.redirects.total`         | Counter | link-service      | shortCode                               |
| `outbox.pending.events`        | Gauge   | link-service      | -                                       |
| `click.events.processed.total` | Counter | analytics-service | status (success / duplicate)            |
| `webhook.deliveries.total`     | Counter | webhook-service   | status (success / failed / circuit_open)|

---

## 3. Database Schema

### PostgreSQL — database: `auth_db`

**users**

| Column        | Type         | Constraints     |
|---------------|--------------|-----------------|
| id            | UUID         | PK              |
| email         | VARCHAR(255) | UNIQUE NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL        |
| created_at    | TIMESTAMP    | NOT NULL        |

**refresh_tokens**

| Column     | Type         | Constraints           |
|------------|--------------|-----------------------|
| id         | UUID         | PK                    |
| user_id    | UUID         | FK -> users, NOT NULL |
| token_hash | VARCHAR(255) | UNIQUE NOT NULL       |
| expires_at | TIMESTAMP    | NOT NULL              |
| revoked    | BOOLEAN      | DEFAULT false         |
| created_at | TIMESTAMP    | NOT NULL              |

---

### PostgreSQL — database: `links_db`

**links**

| Column       | Type        | Constraints           |
|--------------|-------------|-----------------------|
| id           | UUID        | PK                    |
| user_id      | UUID        | NOT NULL              |
| original_url | TEXT        | NOT NULL              |
| short_code   | VARCHAR(50) | UNIQUE NOT NULL       |
| custom_slug  | VARCHAR(50) | NULLABLE              |
| expires_at   | TIMESTAMP   | NULLABLE              |
| max_clicks   | INTEGER     | NULLABLE              |
| click_count  | BIGINT      | NOT NULL DEFAULT 0    |
| is_active    | BOOLEAN     | NOT NULL DEFAULT true |
| created_at   | TIMESTAMP   | NOT NULL              |

**outbox_events**

| Column       | Type        | Constraints          |
|--------------|-------------|----------------------|
| id           | UUID        | PK                   |
| aggregate_id | UUID        | NOT NULL             |
| event_type   | VARCHAR(50) | NOT NULL             |
| payload      | JSONB       | NOT NULL             |
| published    | BOOLEAN     | NOT NULL DEFAULT false|
| created_at   | TIMESTAMP   | NOT NULL             |

---

### PostgreSQL — database: `webhooks_db`

**webhook_configs**

| Column     | Type      | Constraints                 |
|------------|-----------|-----------------------------|
| id         | UUID      | PK                          |
| user_id    | UUID      | NOT NULL                    |
| link_id    | UUID      | NOT NULL                    |
| target_url | TEXT      | NOT NULL                    |
| threshold  | BIGINT    | NOT NULL                    |
| is_active  | BOOLEAN   | NOT NULL DEFAULT true       |
| created_at | TIMESTAMP | NOT NULL                    |
|            |           | UNIQUE (link_id, threshold) |

**webhook_deliveries**

| Column        | Type        | Constraints                     |
|---------------|-------------|---------------------------------|
| id            | UUID        | PK                              |
| config_id     | UUID        | FK -> webhook_configs, NOT NULL |
| triggered_at  | TIMESTAMP   | NOT NULL                        |
| status        | VARCHAR(20) | NOT NULL                        |
| response_code | INTEGER     | NULLABLE                        |
| attempt_count | INTEGER     | NOT NULL DEFAULT 1              |
| last_error    | TEXT        | NULLABLE                        |
| created_at    | TIMESTAMP   | NOT NULL                        |

---

### MongoDB — database: `analytics`

**click_events** collection:
```
eventId, linkId, userId, shortCode, timestamp,
ip, country, city, deviceType, os, browser, referrer
```

---

### Redis key space

| Key pattern                  | Type        | Owner             | Notes                      |
|------------------------------|-------------|-------------------|----------------------------|
| `revoked:{jti}`              | String      | auth-service      | TTL = token remaining life |
| `click:total:{linkId}`       | String      | analytics-service | INCR counter               |
| `click:uniq:{linkId}:{date}` | HyperLogLog | analytics-service | PFADD / PFCOUNT            |

---

## 4. Technologies

| Concern               | Choice                                                    |
|-----------------------|-----------------------------------------------------------|
| Language              | Java 25                                                   |
| Framework             | Spring Boot 4.x                                           |
| Build tool            | Maven (multi-module)                                      |
| API Gateway           | Spring Cloud Gateway                                      |
| Config management     | Spring Cloud Config Server (native filesystem backend)    |
| Persistence (SQL)     | PostgreSQL 17 + Spring Data JPA (Hibernate 7)             |
| DB migrations         | Liquibase (YAML changelog format)                         |
| Persistence (NoSQL)   | MongoDB 8 + Spring Data MongoDB                           |
| Cache / counters      | Redis 8 + Spring Data Redis (Lettuce driver)              |
| Message broker        | RabbitMQ 4 + Spring AMQP                                  |
| Inter-service HTTP    | Spring Cloud OpenFeign                                    |
| Auth tokens           | JWT RS256 — jjwt (io.jsonwebtoken)                        |
| Resilience            | Resilience4j Circuit Breaker                              |
| Validation            | Jakarta Validation (Bean Validation)                      |
| Logging               | Logback + logstash-logback-encoder (structured JSON)      |
| Tracing               | Micrometer Tracing + Brave + Zipkin reporter              |
| Metrics               | Micrometer + micrometer-registry-prometheus               |
| Containerisation      | Docker + Docker Compose                                   |
| CI                    | GitHub Actions                                            |
| Testing               | JUnit 5, Testcontainers, WireMock, AssertJ                |
| Coverage              | JaCoCo (per-module reports)                               |
| Code quality          | SonarQube 26.2 Community (local docker-compose) + sonar-maven-plugin — one Sonar project per service |
| Geo / UA parsing      | ip-api.com HTTP call (WireMocked in tests)                |

---

## 5. Testing Approach

See [`test-approach.md`](test-approach.md) for full conventions, patterns, and anti-patterns.

Key principles:

- **Integration tests over unit tests.** Each service has a test suite exercising the
  full stack from HTTP/AMQP inbound to DB/broker outbound using real infrastructure
  via Testcontainers.
- **One IT class per endpoint/feature.** Each integration test class covers a single
  endpoint with all edge cases (happy path, validation failures, error scenarios).
  No multi-endpoint flow tests.
- **Unit tests** only for pure logic with no infrastructure dependencies.
- **Single shared context per service.** One `AbstractIntegrationTest` base class per
  service declares all containers as `static`, starts them once, and wires properties
  via `@DynamicPropertySource`. All test classes extend it — no context restarts.
- **No Mockito in integration tests.** External HTTP calls (ip-api.com, webhook targets,
  Feign inter-service calls) are stubbed via `WireMockContainer`. Mockito is permitted
  in unit tests only.
- **API utilities encapsulate HTTP calls.** `AuthApiUtils.OK.*` methods assert success
  and return parsed bodies; `AuthApiUtils.Error.*` methods return raw responses for
  custom assertions.
- **Config Server disabled in tests.** Every service module has
  `src/test/resources/application-test.yml` setting `spring.config.import=""`.
  All properties come from `@DynamicPropertySource`.
- **Observability in tests.** Tracing enabled at 100% sample rate. Zipkin/Prometheus
  containers not started — configure Zipkin reporter to a no-op or WireMock stub.
  At least one test per service asserts that `traceId` is present in log output.

### 5.1 Code Quality (SonarQube)

Code quality is enforced by a local SonarQube 26.2 Community server defined in
`docker/docker-compose.yml` under the `sonar` Docker Compose profile (so it does not
start by default — `docker compose --profile sonar up -d sonarqube` to launch).

**One Sonar project per service.** Each Maven module is scanned independently and
appears as its own project in the SonarQube UI, so you can open a single service in
isolation. The project key is derived in the parent POM as
`smart-url-hub-${project.artifactId}` and the project name as `${project.artifactId}`,
so scanning a module with `-pl <module>` automatically produces a uniquely-keyed
Sonar project (e.g., `smart-url-hub-auth-service`,
`smart-url-hub-link-service`).

Coverage is produced per-module by JaCoCo: each module emits `target/jacoco.exec`
(unit, via `prepare-agent`) and `target/jacoco-it.exec` (integration, via
`prepare-agent-integration`). The `report` and `report-integration` goals fire in
the `test` and `verify` phases and write
`target/site/jacoco/jacoco.xml` and `target/site/jacoco-it/jacoco.xml`. Sonar reads
both files via `sonar.coverage.jacoco.xmlReportPaths` declared in the parent POM.

The Sonar scan covers (per service):

- **Code coverage** — JaCoCo unit + integration; quality-gate threshold per Sonar Way
  (≥80% on new code).
- **Code smells & maintainability** — default Sonar Way Java rule set (complexity,
  duplication, naming, dead code).
- **Bugs & reliability** — null checks, resource leaks, broken contracts.
- **Security vulnerabilities & hotspots** — OWASP-style findings (SQL injection,
  weak crypto, hardcoded secrets) and security hotspot review.

Quality gate: **Sonar Way** default — ≥80% coverage on new code, 0 new bugs, 0 new
vulnerabilities, ≤3% duplication on new code, all hotspots reviewed. CI fails when
any service's gate fails (`-Dsonar.qualitygate.wait=true`).

Coverage exclusions (declared in parent POM, so each per-service project inherits
them):
`**/*Application.java`, `**/config/**`, `**/dto/**`, `**/entity/**`, `**/model/**`,
`**/exception/**`.

Local usage:

```powershell
docker compose --profile sonar up -d sonarqube
# First login at http://localhost:9000 — admin / admin, force password change, then
# create a global analysis token under My Account > Security.
$env:SONAR_TOKEN = "<token>"

# Build everything once so all jacoco XMLs exist:
./mvnw verify

# Scan one service in isolation. Use `-f <module>/pom.xml`, not `-pl <module>`:
# sonar-maven-plugin 5.x requires the scanned module to be Maven's execution
# root, which `-pl` does not satisfy (it leaves the parent as the execution
# root and the plugin then fails with "Maven session does not declare a top
# level project").
./mvnw -f auth-service/pom.xml sonar:sonar "-Dsonar.token=$env:SONAR_TOKEN"

# Or scan every service in turn (each creates / updates its own Sonar project):
"common-lib","config-server","auth-service","api-gateway","link-service",`
    "analytics-service","webhook-service" | ForEach-Object {
    ./mvnw -f "$_/pom.xml" sonar:sonar "-Dsonar.token=$env:SONAR_TOKEN"
}
```

---

## 6. Implementation Plan

### Stage Status

- [x] Stage 1 — Project Scaffolding & Infrastructure
- [x] Stage 2 — Config Server
- [x] Stage 3 — common-lib Module
- [x] Stage 4 — Auth Service
- [x] Stage 5 — API Gateway
- [x] Stage 6 — Link Service
- [x] Stage 7 — Analytics Service
- [ ] Stage 8 — Webhook Service
- [ ] Stage 9 — Docker Compose Integration & Final Wiring
- [x] Stage 10 — CI Pipeline

---

### Stage 1 — Project Scaffolding & Infrastructure

Set up the Maven multi-module parent POM with dependency and plugin management for all
libraries used across the project. Scaffold each module with a minimal `pom.xml` and
empty main class. Create `docker/docker-compose.yml` with all infrastructure containers:
PostgreSQL, MongoDB, Redis, RabbitMQ, Zipkin, and Prometheus. Include health checks on
every container. Add `docker/prometheus/prometheus.yml` with scrape targets for all
application services (by container name). Create the `config/` directory with empty
placeholder YAML files. Create a root `.env` with placeholder secrets.

Add the `sonarqube` and `sonar-db` services to `docker-compose.yml` under the `sonar`
Docker Compose profile so they only start on demand (`docker compose --profile sonar
up -d sonarqube`). Add `SONAR_DB_USER` / `SONAR_DB_PASSWORD` / `SONAR_DB_NAME` /
`SONAR_HOST_URL` / `SONAR_TOKEN` to `.env`. Configure `jacoco-maven-plugin` and
`sonar-maven-plugin` in the parent `<pluginManagement>` and activate JaCoCo for all
modules. In the parent `<properties>` set `sonar.projectKey` to
`smart-url-hub-${project.artifactId}` so each Maven module becomes its own Sonar
project automatically when scanned with `-pl`.

**Done when:** `docker compose up -d` brings all infrastructure containers to healthy,
`docker compose --profile sonar up -d sonarqube` brings SonarQube to healthy on
http://localhost:9000, and `./mvnw verify` produces per-module
`target/site/jacoco/jacoco.xml` files.

---

### Stage 2 — Config Server

Implement `config-server` using `@EnableConfigServer` with a native filesystem backend
pointing to the `config/` directory (mount path configurable via `CONFIG_REPO_PATH`).

Populate `config/application.yml` with shared properties for all services:
```yaml
spring:
  rabbitmq: ...
  data.redis: ...
management:
  tracing.sampling.probability: 1.0
  zipkin.tracing.endpoint: http://zipkin:9411/api/v2/spans
  endpoints.web.exposure.include: health, info, prometheus, metrics
  metrics.tags.service: ${spring.application.name}
```

Populate per-service YAML files with service-specific overrides (port, datasource URL,
app name). Add config-server to `docker-compose.yml` on the internal network only, with
a health check. All other application services will declare `depends_on: config-server`.

Write integration tests that verify shared config, per-service config, and graceful
handling of unknown service names.

**Done when:** `GET /link-service/default` returns expected properties from `config/link-service.yml`.

---

### Stage 3 — common-lib Module

Implement the shared Spring Boot auto-configured starter consumed by all servlet-based
services: auth-service, link-service, analytics-service, webhook-service. The
api-gateway is reactive (WebFlux) and pulls observability dependencies directly rather
than via common-lib.

Provide:

- **`AuthenticatedUser` POJO** — userId, email, roles.
- **`GatewayAuthFilter`** — servlet `OncePerRequestFilter` that reads `X-User-Id`,
  `X-User-Email`, `X-User-Roles` headers, builds `Authentication`, sets on
  `SecurityContextHolder`. No-op when headers are absent.
- **Default `SecurityFilterChain`** — STATELESS, CSRF off, `/actuator/health` public,
  all else authenticated. `@ConditionalOnMissingBean(SecurityFilterChain.class)` so
  services can override (auth-service does, to whitelist its public `/auth/**` endpoints).
- **Global error handling** — `@RestControllerAdvice` returning RFC 7807 Problem Details
  (`application/problem+json`). Must handle at minimum:
  - `MethodArgumentNotValidException` → 400 with field-level validation errors in the
    `errors` extension field.
  - `AccessDeniedException` → 403.
  - Unhandled `Exception` → 500, without leaking stack traces to the client.
- **`logback-common.xml`** — included by each consuming service's own
  `logback-spring.xml`. JSON appender (logstash-logback-encoder) for non-test profiles;
  pattern appender for test profile. Fields must include `traceId` and `spanId` from MDC.
- Micrometer Tracing, Zipkin reporter, and Prometheus registry as transitive dependencies
  so consuming services get observability auto-configured.
- `spring-cloud-starter-config` as transitive so consuming services pick up the
  Config Server import.

Register via `AutoConfiguration.imports`. Write unit tests for the filter and the
error handler (verify correct HTTP status and Problem Details structure per exception
type), plus a `@SpringBootTest` slice with a stub controller asserting the starter
auto-wires correctly.

---

### Stage 4 — Auth Service

Implement user registration, login, RS256 JWT issuance, refresh token rotation, and
logout with Redis-based revocation. Auth-service depends on common-lib but overrides
the default `SecurityFilterChain` to whitelist its `/auth/**` endpoints (the gateway
does not validate JWTs for `/auth/**` routes, so X-User-* headers are not injected for
this service — token introspection happens in auth-service per-endpoint).

Key points:
- Apply Jakarta Validation on register (`@NotBlank` email, password min length) and
  login request DTOs. 400 Problem Details on violation comes for free from common-lib's
  `@RestControllerAdvice`.
- Load RSA private + public key from paths defined in `config/auth-service.yml`.
- Access tokens: RS256, 15-min expiry, include `jti` (UUID).
- Refresh tokens: opaque UUID stored hashed in PostgreSQL.
- `POST /auth/logout` writes `revoked:{jti}` to Redis with TTL = remaining token life.
  Extracts user/jti by validating the bearer token itself (the gateway treats `/auth/**`
  as public and does not pre-validate).
- `GET /auth/public-key` exposes PEM public key (public endpoint).
- Liquibase YAML changelogs for database `auth_db` (see Section 3 for table definitions).
- Provide a `logback-spring.xml` that `<include>`s `logback-common.xml` from common-lib.
- Override `SecurityFilterChain` bean to permit all `/auth/**` requests.

Integration tests: full register → login → refresh → logout flow; assert `jti` in Redis
after logout; assert revoked refresh token rejected; assert invalid input returns 400.

---

### Stage 5 — API Gateway

Implement JWT validation, Redis revocation check, and identity header injection as a
`GlobalFilter`. Route configuration lives in `config/api-gateway.yml`.

Key points:
- Public routes (no JWT required): `POST|GET /auth/**`, `GET /{shortCode}`.
- All other routes: validate RS256 signature with public key, check Redis `EXISTS revoked:{jti}`.
- On success: inject `X-User-Id`, `X-User-Email`, `X-User-Roles` into forwarded request.
- On failure: return 401 Problem Details, do not forward.
- Micrometer Tracing instruments the gateway HTTP client automatically — confirm B3
  headers propagate to downstream services.

Integration tests with WireMock stubs for all downstream services: valid JWT forwarded
with correct headers; expired/revoked JWT rejected; public routes bypass filter.

---

### Stage 6 — Link Service

Implement link CRUD and the public redirect endpoint with the Outbox pattern.

Key points:
- Apply Jakarta Validation on `POST /links` (non-blank original URL, valid URL format,
  custom slug pattern if provided: alphanumeric + hyphens only).
- `GET /{shortCode}` — resolve link, return 410 if expired/inactive/max_clicks reached,
  otherwise write `ClickEvent` to `outbox_events` in the same transaction, increment
  `click_count`, return 302. Register `link.redirects.total` counter here.
- Outbox poller `@Scheduled` (2s fixed delay): `SELECT … FOR UPDATE SKIP LOCKED LIMIT 50`,
  publish to `click.events` exchange, mark published — all in one `@Transactional`.
- Register `outbox.pending.events` gauge backed by unpublished row count.
- Liquibase YAML changelogs for database `links_db` (see Section 3).
- Depends on common-lib for security and error handling.

Integration tests: redirect flow, outbox row written, poller publishes to RabbitMQ,
gauge and counter values correct, invalid `POST /links` returns 400 Problem Details.

---

### Stage 7 — Analytics Service

Implement the click event consumer with enrichment, persistence, Redis counters, and
threshold detection.

Key points:
- On `ClickEvent` consumed: check MongoDB for duplicate `eventId` (ack + skip if found);
  call ip-api.com for geo; parse UA; persist to MongoDB; `INCR click:total:{linkId}`;
  `PFADD click:uniq:{linkId}:{date}`.
- After INCR: use a **Feign client** to call
  `GET http://webhook-service:8084/internal/webhooks/thresholds?linkId=`
  — if counter matches any threshold, publish `ThresholdReachedEvent` to `threshold.reached`.
- Feign client interface is defined in analytics-service (not shared) to avoid coupling.
  Configure connect and read timeouts in `config/analytics-service.yml`.
- B3 trace propagation via `MessagePostProcessor` on AMQP consumer.
- Register `click.events.processed.total` counter (tags: `success`, `duplicate`).
- REST read API: `GET /analytics/links/{linkId}/summary` and `.../timeseries`.
- Depends on common-lib.

Integration tests with WireMock stubs for ip-api.com and webhook-service internal
endpoint (Feign resolves the base URL from config — point it to WireMock in tests via
`@DynamicPropertySource`).

---

### Stage 8 — Webhook Service

Implement webhook config management and threshold-triggered HTTP delivery with Circuit
Breaker.

Key points:
- Apply Jakarta Validation on `POST /webhooks` (non-blank target URL, valid URL format,
  positive threshold value).
- `POST /webhooks` — validate caller owns the link by calling link-service via a
  **Feign client** (`GET /links/{linkId}`, verify `userId` matches `X-User-Id`).
  Feign client interface defined in webhook-service (not shared).
  Configure connect and read timeouts in `config/webhook-service.yml`.
- `GET /internal/webhooks/thresholds?linkId=` — internal, no auth, used by analytics-service.
- On `ThresholdReachedEvent` consumed: idempotency check via `webhook_deliveries`;
  fire HTTP POST to `target_url` via Resilience4j Circuit Breaker `webhook-delivery`;
  persist delivery record with status (`SUCCESS` / `FAILED` / `DEAD`).
- DLQ consumer: log + persist `DEAD` record; do not re-publish.
- B3 trace propagation via `MessagePostProcessor` on AMQP consumer.
- Register `webhook.deliveries.total` counter (tags: `success`, `failed`, `circuit_open`).
- Resilience4j CB config in `config/webhook-service.yml` (window 10, threshold 50%,
  open 30s).
- Liquibase YAML changelogs for database `webhooks_db` (see Section 3).
- Depends on common-lib.

Integration tests with WireMock stubs for target URLs and link-service Feign client.
Verify CB opens after N failures (small window via `application-test.yml` override).
Verify invalid `POST /webhooks` returns 400 Problem Details.

---

### Stage 9 — Docker Compose Integration & Final Wiring

Containerise all services and verify the full stack runs end to end.

Key points:
- Multi-stage `Dockerfile` per module (Maven build stage + JRE runtime stage, non-root user).
- Add all application services to `docker-compose.yml`. Two networks: `hub-internal`
  (all services, `internal: true`) and `hub-external` (api-gateway only, exposes port 8080).
- Mount RSA key pair as a Docker volume shared between auth-service and api-gateway.
- `docker-compose.override.yml` for dev mode — infra + config-server only; services
  run on host JVM with `application-local.yml` overriding hostnames to `localhost`.
- Write `README.md` covering: RSA key pair generation, full stack startup, observability
  URLs (Zipkin `:9411`, Prometheus `:9090`), curl smoke test walkthrough, and per-module
  test command.

**Done when:** smoke test passes — register → login → create link → N redirects →
webhook delivery visible → distributed trace visible in Zipkin UI.

---

### Stage 10 — CI Pipeline

Create a GitHub Actions workflow at `.github/workflows/ci.yml`.

Key points:
- Trigger on push and pull request to `main`.
- Single job with the following steps:
  1. Checkout with `fetch-depth: 0` — Sonar needs full git history for accurate
     new-code detection and blame.
  2. Set up Java 25 and cache `~/.m2/repository` and `~/.sonar/cache`.
  3. Start a SonarQube service container (`sonarqube:26.2.0.119303-community` + a Postgres
     service container) so the scan does not depend on any external host. Wait for
     `/api/system/status` to report `UP`, then bootstrap a global analysis token via
     the Sonar Web API (using the default `admin`/`admin` credentials, then changing
     the password) and export it as `SONAR_TOKEN` for subsequent steps.
  4. Run `./mvnw verify` — compiles all modules, runs unit + integration tests,
     emits per-module `jacoco.exec` / `jacoco-it.exec` and the corresponding XML
     reports.
  5. Scan each service module in turn so each one creates / updates its own Sonar
     project. Recommended pattern: a GitHub Actions matrix job (one entry per
     module: `common-lib`, `config-server`, `auth-service`, `api-gateway`,
     `link-service`, `analytics-service`, `webhook-service`) running
     `./mvnw -f ${{ matrix.module }}/pom.xml sonar:sonar
     -Dsonar.host.url=http://localhost:9000
     -Dsonar.token=$SONAR_TOKEN
     -Dsonar.qualitygate.wait=true`. (Use `-f <module>/pom.xml` rather than
     `-pl <module>` — sonar-maven-plugin 5.x requires the scanned module to be
     Maven's execution root, which `-pl` does not satisfy.) A matrix is preferable to a shell loop because
     each service's gate failure is reported as a separate failed job rather than
     short-circuiting the rest. Alternative if a matrix is overkill: a single shell
     loop that records each module's gate result and fails the job at the end if any
     failed.
- The `qualitygate.wait` flag makes the Maven invocation fail when the Sonar Way
  gate fails for that service (coverage <80% on new code, new bugs, new
  vulnerabilities, unreviewed hotspots, etc.).
- Testcontainers requires Docker — GitHub Actions runners have Docker available by
  default; no extra setup needed.
- The bootstrap `SONAR_TOKEN` is generated at runtime, so the workflow does not
  require any pre-provisioned repository secret.
- If any module's tests fail OR any service's Sonar quality gate fails, the workflow
  fails.

**Done when:** a push to `main` triggers the workflow, all tests pass, every
service's Sonar quality gate passes, and each service appears as its own project in
the SonarQube UI.

---

## 7. Open Questions

| #  | Question                                                 | Resolution                          |
|----|----------------------------------------------------------|-------------------------------------|
| 1  | One Postgres container or separate containers?           | One container, per-service databases (`auth_db`, `links_db`, `webhooks_db`) created via init script |
| 2  | DB migration tool and format?                            | Liquibase, YAML format              |
| 3  | Roles in DB or hardcoded?                                | Always ROLE_USER                    |
| 4  | Rate limiting in gateway?                                | Skip for now                        |
| 5  | Enforce max_clicks at redirect?                          | Yes                                 |
| 6  | Analytics threshold lookup — REST or local cache?        | Feign call to webhook-service       |
| 7  | Threshold fires once or at every multiple?               | Exactly once per threshold per link |
| 8  | docker-compose.override.yml for dev mode?                | Yes                                 |
| 9  | Config-server auth or encryption?                        | No — internal network is sufficient |
| 10 | DLQ automatic retry?                                     | No — dead-letter logging only       |
| 11 | Feign client interfaces — shared artifact or per-consumer? | Per-consumer (no shared module)   |
| 12 | Feign timeout values?                                    | Connect 2s / Read 5s (default)      |

---

## 8. Suggestions for Improvement

Areas identified during code review that should be addressed before production deployment.

### 8.1 Rate Limiting on Authentication Endpoints (High Priority)

`/auth/login` and `/auth/register` have no rate limiting — they are vulnerable to brute
force and credential stuffing attacks. Implement at the API Gateway level using Spring
Cloud Gateway's built-in `RequestRateLimiter` filter backed by Redis (token bucket
algorithm). Suggested limits:
- `/auth/login`: 10 requests/minute per IP
- `/auth/register`: 5 requests/minute per IP
- `/auth/refresh`: 20 requests/minute per IP

This revisits Open Question #4 — rate limiting should no longer be skipped.

### 8.2 Config Server Startup Strategy (Medium Priority)

Services currently use `optional:configserver:` import combined with `fail-fast: true`,
which is contradictory. Two options:
- **Option A (recommended):** Remove `optional:` prefix — services fail fast and
  explicitly if config-server is unavailable. This makes deployment order clear.
- **Option B:** Set `fail-fast: false` and provide local fallback properties in each
  service's `application.yml` so services can start in degraded mode during local
  development without config-server.

### 8.3 MongoDB Application User (Medium Priority)

`analytics-service.yml` connects to MongoDB using `MONGO_INITDB_ROOT_USERNAME` /
`MONGO_INITDB_ROOT_PASSWORD` — root credentials for an application service. Create a
scoped application user with read/write access only to the `analytics` database:
- Add `docker/mongo/init/01-create-app-user.js` that creates a user with
  `readWrite` role on the `analytics` database.
- Update `analytics-service.yml` to use the new app credentials.

### 8.4 JWT Key Mounting in Docker Compose (Required for Stage 9)

Configuration references `/run/secrets/auth-jwt/private.pem` and `/public.pem`, but
`docker-compose.yml` has no volume or Docker secret definition for these files. When
containerising application services (Stage 9):
- Generate RSA key pair during project setup (gitignored).
- Mount keys via Docker Compose volumes or use Docker secrets.
- Add a `docker/scripts/generate-keys.sh` helper.

### 8.5 Structured Logging in Tests (Low Priority)

Tests currently produce verbose plain-text logs. Add a `logback-test.xml` to each
service's `src/test/resources/` with a simplified pattern (e.g., `%level %logger{20} -
%msg%n`) to reduce CI noise and improve readability.

### 8.6 Service Health Probes in Docker Compose (Required for Stage 9)

Application services (api-gateway, auth-service, link-service, analytics-service,
webhook-service) have no healthcheck definitions in `docker-compose.yml`. Add Spring Boot
Actuator-based health checks:
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:<port>/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 30s
```
Combine with `depends_on.<service>.condition: service_healthy` to enforce startup ordering.
