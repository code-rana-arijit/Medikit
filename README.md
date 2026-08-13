# MediKit

> **An AI-powered Blinkit clone purpose-built for medicine delivery** - a production-ready, highly scalable microservices platform designed to handle **50,000 concurrent users**.

MediKit brings the instant-grocery (Blinkit/Zomato Instant) experience to the healthcare vertical. Browse medicines, upload prescriptions, order with lightning delivery, and track everything in real time - powered by an event-driven microservices architecture.

![Build](https://img.shields.io/badge/build-passing-brightgreen) ![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green) ![License](https://img.shields.io/badge/license-MIT-blue)

---

## Table of Contents

- [Why MediKit](#why-medikit)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Microservices](#microservices)
- [High-Level Design: The Order Saga](#high-level-design-the-order-saga)
- [Phase-wise Development Plan](#phase-wise-development-plan)
- [Scaling to 50,000 Concurrent Users](#scaling-to-50000-concurrent-users)
- [Getting Started](#getting-started)
- [Running Locally (Docker Compose)](#running-locally-docker-compose)
- [Deploying to Kubernetes](#deploying-to-kubernetes)
- [CI/CD Pipeline](#cicd-pipeline)
- [API Overview](#api-overview)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)

---

## Why MediKit

Traditional medicine delivery platforms fail on three fronts: **speed**, **inventory accuracy**, and **regulatory compliance** (prescription validation). MediKit is engineered from the ground up to solve all three:

1. **Instant delivery promise** - A delivery slot + real-time tracking system that models pharmacy-to-customer logistics like Blinkit does.
2. **Oversell prevention** - A distributed inventory reservation system with pessimistic locking and saga compensation to guarantee you never sell what you don't have.
3. **Compliance built-in** - A dedicated prescription service that validates prescription uploads before dispatching schedule-H drugs.

---

## Features

### Customer
- Phone/email registration, JWT auth, OTP verification, refresh tokens
- Medicine catalog with categories, trending products, price filters
- Fast search with autocomplete (Redis inverted index, pluggable to Elasticsearch)
- **Generic alternatives** - find cheaper same-salt alternatives
- Persistent shopping cart (Redis-backed, 72h TTL, single-pharmacy enforcement)
- Multiple saved addresses with geolocation
- **Prescription upload & validation workflow**
- Order lifecycle: create → pay → confirm → deliver → complete
- Real-time delivery tracking
- Email/SMS/push notifications
- **AI Health Intelligence** - drug interaction checker, symptom-based recommendations, prescription analysis
### Pharmacy / Admin
- Product & category management
- Inventory/stock management with low-stock alerts
- Delivery slot configuration
- Order management per pharmacy
- Prescription validation queue

### Platform
- API Gateway with JWT security, rate limiting, circuit breaking
- Service discovery (Eureka) & centralized config
- Event-driven integration via Kafka
- Horizontal autoscaling (HPA)
- CI/CD with automated tests, image builds, and deployments

---

## System Architecture

```mermaid
graph TD
    A["Client (App / Web)"] --> G["API Gateway (Port 8080)"]
    G --> D["Discovery Server (Eureka)"]
    G --> C["Config Server"]
    G -->|"/api/v1/auth|users"| US["User Service"]
    G -->|"/api/v1/products"| PS["Product Service"]
    G -->|"/api/v1/search"| SS["Search Service"]
    G -->|"/api/v1/health"| HS["Health Service"]
    G -->|"/api/v1/loyalty"| LS["Loyalty Service"]
    G -->|"/api/v1/discounts"| DS["Discount Service"]
    G -->|"/api/v1/inventory"| IS["Inventory Service"]
    G -->|"/api/v1/cart"| CS["Cart Service"]
    G -->|"/api/v1/orders"| OS["Order Service"]
    G -->|"/api/v1/payments"| PayS["Payment Service"]
    G -->|"/api/v1/delivery"| DelS["Delivery Service"]
    G -->|"/api/v1/prescriptions"| PreS["Prescription Service"]
    G -->|"/api/v1/notifications"| NS["Notification Service"]

    OS -->|"Feign + CircuitBreaker"| IS
    OS -->|"Feign + CircuitBreaker"| PayS
    IS --> PubSub["Kafka (Event Bus)"]
    OS --> PubSub
    PayS --> PubSub
    DelS --> PubSub
    PS --> PubSub
    SS --> PubSub
    NS --> PubSub
    LS --> PubSub

    LS -->|"issue code"| DS
    OS -->|"validate + redeem"| DS
    PubSub -->|"ORDER_CONFIRMED"| DelS
    PubSub -->|"DELIVERY_UPDATED"| OS

    US --> PG1[("PostgreSQL x7 (one per service)")]
    PS --> PG1
    IS --> PG1
    OS --> PG1
    PayS --> PG1
    DelS --> PG1
    PreS --> PG1
    CS --> R[("Redis")]
    IS --> R
    SS --> R
    US --> R
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language / Runtime | Java 17, Spring Boot 3.3 |
| Cloud Frameworks | Spring Cloud (Gateway, Netflix Eureka, Config, OpenFeign), Resilience4j |
| Databases | PostgreSQL 16 (per-service schema), Redis 7 (cache/cart/session/rate-limit) |
| Messaging | Apache Kafka (event bus / saga) |
| Search | Redis inverted index (Elasticsearch-ready abstraction) |
| Containerization | Docker, multi-stage builds, `eclipse-temurin:17-jre-alpine` |
| Orchestration | Kubernetes (Deployments, Services, StatefulSets, HPA, Ingress, PVC) |
| CI/CD | GitHub Actions (build → test → SonarQube → Docker push → kubectl deploy) |
| API Docs | Springdoc OpenAPI 3 / Swagger UI per service |
| Testing | JUnit 5, Mockito |

---

## Microservices

| # | Service | Port | Responsibility | Database |
|---|---------|------|----------------|----------|
| 1 | `api-gateway` | 8080 | Routing, JWT auth, rate limiting, circuit breaking | Redis |
| 2 | `discovery-server` | 8761 | Eureka service registry | - |
| 3 | `config-server` | 8888 | Centralized config (Git-backed) | - |
| 4 | `user-service` | 8101 | Auth (JWT/OTP), profiles, addresses | medikit_users |
| 5 | `product-service` | 8102 | Catalog, categories, pricing, alternatives | medikit_products |
| 6 | `inventory-service` | 8103 | Stock, reservation, deduction, release | medikit_inventory |
| 7 | `cart-service` | 8104 | Redis shopping cart | Redis |
| 8 | `order-service` | 8105 | Order state machine + saga orchestrator | medikit_orders |
| 9 | `payment-service` | 8106 | Mock gateway, idempotency, capture, refund | medikit_payments |
| 10 | `delivery-service` | 8107 | Slots, partner assignment, tracking | medikit_delivery |
| 11 | `notification-service` | 8108 | Email/SMS/push (mock providers) | - |
| 12 | `prescription-service` | 8109 | Prescription upload/validation/expiry | medikit_prescriptions |
| 13 | `search-service` | 8110 | Redis search index, autocomplete | Redis |
| 14 | `health-service` | 8111 | AI health intelligence: drug interactions, symptom analysis, prescription analysis, assistant chat | medikit_health |
| 15 | `loyalty-service` | 8112 | Points ledger, tier progression, redemptions | medikit_loyalty |
| 16 | `discount-service` | 8113 | Discount codes: issue, validate, redeem lifecycle + promotion campaigns | medikit_discounts |
| 17 | `distributor-service` | 8114 | Distributor portal: B2B wholesale supply + B2C retail fulfillment | medikit_distributors |
---

## High-Level Design: The Order Saga

Orders are handled through a **choreographed + orchestrated saga** to keep consistency across services without distributed transactions:

```mermaid
sequenceDiagram
    participant C as Customer
    participant OS as Order Service
    participant IS as Inventory Service
    participant PS as Payment Service
    participant DS as Delivery Service
    participant NS as Notification Service

    C->>OS: POST /api/v1/orders (create)
    OS->>OS: Save order (CREATED)
    OS->>IS: Reserve stock (Feign, retry + circuit breaker)
    alt Reservation fails
        IS-->>OS: FAILED
        OS->>NS: ORDER_FAILED
        NS-->>C: "Order failed"
    else Reservation OK
        OS->>PS: Initiate payment
        alt Payment fails (or COD)
            OS->>IS: Release stock (compensation)
            OS->>NS: ORDER_FAILED
        else Payment success
            OS->>OS: Mark CONFIRMED
            OS-->>Kafka: ORDER_CONFIRMED
            Kafka-->>DS: Create delivery + book slot
            Kafka-->>NS: Notify customer
            DS-->>Kafka: DELIVERY_UPDATED
            Kafka-->>OS: Update order status
        end
    end
```

**Resilience mechanisms:**
- Inventory reservations use `@Lock(PESSIMISTIC_WRITE)` to prevent oversell at the DB level
- Order-service calls inventory/payment through Feign wrapped in Resilience4j **retry + circuit breaker**
- A `SagaTimeoutScheduler` periodically compensates stale orders stuck in `CREATED`/`PENDING_PAYMENT`
- Payment service is **idempotent** (Redis lock + unique `merchantRefId`)
- All events publish to Kafka; consumers use at-least-once with Redis dedup where needed

---

## Phase-wise Development Plan

This is a huge project - here is exactly how it is being (and should be) delivered:

### Phase 0 - Foundation (DONE)
- Repository setup, parent POM, module structure
- Docker Compose for infra: PostgreSQL, Redis, Kafka, Elasticsearch, MailHog
- Development conventions (`docs/DEVELOPMENT_GUIDE.md`)

### Phase 1 - Platform Services (DONE)
- `discovery-server` (Eureka) ✅
- `config-server` (Git-backed) ✅
- `api-gateway` (routing + JWT + rate limiting) ✅

### Phase 2 - Catalog & Users (DONE)
- `user-service` - auth/JWT/OTP, profiles, addresses ✅
- `product-service` - catalog, categories, alternatives ✅
- `inventory-service` - stock + reservation ✅
- `search-service` - Redis index + autocomplete ✅

### Phase 3 - Commerce (DONE)
- `cart-service` - Redis cart ✅
- `order-service` - state machine + saga ✅
- `payment-service` - mock gateway + idempotency ✅

### Phase 4 - Delivery & Engagement (DONE)
- `delivery-service` - slots + tracking ✅
- `notification-service` - email/SMS/push ✅
- `prescription-service` - upload/validation ✅

### Phase 5 - Kubernetes & CI/CD (DONE)
- Full K8s manifests (StatefulSets, Deployments, HPA, Ingress, ConfigMaps, Secrets) ✅
- GitHub Actions: test → SonarQube → Docker → kubectl deploy ✅

### Phase 6 - Hardening for 50K Concurrent Users (IN PROGRESS)
- [x] Kafka multi-partition tuning + consumer groups per service
- [x] Redis Cluster mode (cart/lock/session sharding)
- [x] PostgreSQL read replicas + connection pooling (PgBouncer)
- [x] Introduce real Elasticsearch for full-text search
- [x] End-to-end load testing (k6/Gatling), capacity model per service
- [x] Observability: Prometheus + Grafana dashboards, distributed tracing (Micrometer Tracing + Tempo), Loki logs
- [ ] Envoy/Istio service mesh, mTLS
- [ ] Chaos engineering (Chaos Mesh) for saga resilience validation
- [x] Kubernetes: PodDisruptionBudgets, NetworkPolicies, priority classes
- [x] Rate limiting at gateway with token-bucket per user + per IP
- [x] Caching strategy: product catalog TTL cache, hot product local cache (Caffeine)

### Phase 7 - Production Compliance (DONE)
- [x] Real payment gateway integration (Razorpay/Stripe)
- [x] Real SMS/email providers (Twilio, SES)
- [x] S3-compatible storage for prescription images
- [x] Pharmacist verification, Drug license validation
- [x] GDPR/HIPAA-style audit logging, data retention policies
- [x] Disaster recovery: cross-region replication, RPO/RTO targets

### Phase 8 - AI Health Intelligence (DONE)
- [x] **Drug interaction checker** - `health-service` with a seeded interaction KB (salt pairs, severity, clinical advice); resolves brand names to salts, flags contraindicated/major interactions
- [x] **Symptom-based medicine recommender** - rule-based symptom → condition → medicine reasoning engine with ranked suggestions + safety disclaimers
- [x] **Prescription OCR & AI validation** - medical-entity extraction from prescription text (OCR-ready), resolves brands to salts, cross-checks against order items and the interaction KB
- [x] **AI assistant chat API** - intent detection + plain-English clinical summaries over symptom/interaction/prescription analysis; rule-based composer by default, optional env-gated LLM backend

### Phase 9 - Engagement & Retention
#### Phase 9a - Loyalty Program (DONE)
- [x] **Points ledger** - `loyalty-service` earns points on every confirmed order (`ORDER_CONFIRMED` event); idempotent by `orderId` so Kafka at-least-once replays are safe
- [x] **Tier progression** - BRONZE → SILVER → GOLD → PLATINUM by cumulative spend, with earn multipliers (1.0 / 1.1 / 1.25 / 1.5)
- [x] **Redemption** - users redeem points for in-memory `MEDIKIT-` discount codes; unit-based ratio (100 pts → ₹10)

#### Phase 9b - Reward Coupon Engine (DONE)
- [x] **Discount code lifecycle** - `discount-service` owns codes: issue, validate, redeem (single-use, owned by user, 30-day expiry)
- [x] **Loyalty redemption integration** - `loyalty-service` issues redemption codes via `discount-service` (Feign), with local fallback if it is unavailable
- [x] **Checkout discount** - `order-service` accepts an optional `discountCode`; validated against `discount-service` and applied to the order total
- [x] **Promotion campaigns** - `discount-service` campaigns with batch-issued codes: fixed amount or percentage-off, with first-order-only flags

#### Phase 9d - Referral Program (DONE)
- [x] **Referral codes** - every loyalty account gets a unique `REF-` code; `GET /api/v1/loyalty/referral` returns it with a shareable URL
- [x] **Referral registration** - `POST /api/v1/loyalty/referrals/register` binds a user to a referrer (idempotent, self-referral guard)
- [x] **Referrer rewards** - when the referred user's first order is confirmed, the referrer earns a points bonus (once), recorded as a `REFERRAL` ledger transaction

### Phase 10 - Distributor Portal (DONE)
#### Phase 10a - Distributor Onboarding & B2B Supply
- [x] **Distributor role** - `user-service` gains a `DISTRIBUTOR` role; any medical shop can sign in and self-upgrade via `PUT /api/v1/users/me/role`
- [x] **Distributor profile** - `distributor-service` lets shops register shop name, license, address and city; public directory lists active distributors
- [x] **Wholesale catalog** - distributors maintain a B2B catalog (product, pack size, unit price, stock) with add/update/delete operations
- [x] **B2B supply orders** - shops place wholesale stock orders (`DB2B-` order numbers) with inventory deduction, and full status workflow: PENDING → CONFIRMED → SHIPPED → DELIVERED (cancellation restores stock)

#### Phase 10b - B2C Retail Fulfillment
- [x] **Retail fulfillment** - distributors claim customer retail orders via `order-service` (Feign), tracking CLAIMED → PICKED_UP → IN_TRANSIT → DELIVERED
- [x] **Fulfillment guardrails** - one distributor per order, self-order rejection, strict status transitions, cross-distributor access control

### Phase 11 - Delivery Partner Portal (DONE)
- [x] **Delivery partner role** - `user-service` supports `DELIVERY_PARTNER`; admins grant the role via `GET /api/v1/admin/users?q=` and `PUT /api/v1/admin/users/{id}/role`
- [x] **Claim pool** - `GET /api/v1/delivery/available` lists unassigned PENDING deliveries; partners claim one with `POST /api/v1/delivery/{orderId}/claim` (guardrails: no double-claim, cancelled deliveries rejected)
- [x] **Partner deliveries** - `GET /api/v1/delivery/partner?status=` lists the partner's deliveries; `PUT /api/v1/delivery/{orderId}/status` is role-guarded so only the assigned partner can advance status (ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED)
- [x] **Live location sharing** - partners publish live coordinates via `PUT /api/v1/delivery/{orderId}/location` (assigned-partner only, rejected after DELIVERED/CANCELLED); customers see the partner's position and a live badge on the order detail, polling every 8s
- [x] **Partner portal UI** - `/partner` dashboard with status stats, available-delivery claim queue, and my-deliveries workflow with manual or browser-geolocation location sharing

---

## Scaling to 50,000 Concurrent Users

The architecture is designed for this from day one. Here is the strategy:

### Capacity Model (per service, at 50K concurrent users)
Assumes ~2,000 requests/sec ingress, ~500 orders/sec peak:

| Service | Est. TPS | Replicas (HPA) | Bottleneck | Mitigation |
|---------|----------|----------------|------------|------------|
| api-gateway | 2,000 | 10-20 | CPU | Stateless, scales horizontally |
| user-service | 400 | 3-30 | DB writes | Indexed lookups, Redis fail counters |
| product-service | 1,200 | 3-30 | DB reads | Redis cache (3600s TTL) |
| inventory-service | 700 | 3-20 | DB row locks | PESSIMISTIC_WRITE, short txns, Redis locks |
| cart-service | 600 | 3-20 | Redis ops | Redis single ops, per-user locks |
| order-service | 500 | 3-20 | Saga orchestration | Async Kafka, retries, timeouts |
| payment-service | 500 | 2-15 | Idempotency | Redis idempotency keys |
| search-service | 800 | 2-10 | Redis set ops | Inverted index, token sets |
| health-service | 100 | 2-8 | KB lookups | Indexed pairs, pre-resolved salts, condition cache |
| loyalty-service | 200 | 2-8 | DB writes | Indexed orderId/userId, idempotent awards |
| discount-service | 300 | 2-8 | DB reads | Unique code lookup, active-only scans |
| distributor-service | 300 | 2-8 | DB writes | Indexed buyer/distributor/order-number, stock rows locked on B2B order |

### Key Scalability Decisions
1. **Database per service** - independent scale, no cross-service joins, no shared locks
2. **Event-driven decoupling** - Kafka decouples order from delivery/notification; burst absorption
3. **Stateless services** - all state lives in Redis/PostgreSQL/Kafka, so any pod can serve any request
4. **Horizontal autoscaling** - HPA on CPU + memory with aggressive scale-up, conservative scale-down
5. **Connection pooling** - HikariCP tuned (`maxPoolSize=50`), Redis Lettuce pool
6. **Caching** - product/category data cached in Redis; categories have 2h TTL
7. **Rate limiting** - gateway token bucket per user/IP (Redis-backed)
8. **Circuit breakers** - resilient to downstream failures (inventory/payment)
9. **Batch writes** - Hibernate `order_inserts`, batch_size config on write-heavy services

> **Note on PostgreSQL for 50K users:** For truly production-scale, run PostgreSQL with read replicas (product/search-heavy services) and PgBouncer in front. Partition high-volume tables (`orders`, `order_items`) by time.

---

## Getting Started

### Prerequisites
- JDK 17
- Maven 3.8+
- Docker & Docker Compose
- (Optional) kubectl + a Kubernetes cluster

### 1. Build everything
```bash
# Build all services
mvn clean package

# Build & run tests
mvn test
```

### 2. Start infrastructure
```bash
# Start postgres, redis, kafka, elasticsearch, mailhog
./scripts/dev.sh infra
```

### 3. Start all services
```bash
./scripts/dev.sh all
```

### 4. Access services
- Gateway / API: `http://localhost:8080`
- Eureka dashboard: `http://localhost:8761`
- Config server: `http://localhost:8888`
- Swagger per service: `http://localhost:<port>/swagger-ui.html`
- MailHog (capture emails): `http://localhost:8025`

---

## Running Locally (Docker Compose)

```bash
# Start just infrastructure
docker compose --profile infra up -d

# Start infrastructure + all 14 services (first build may take several minutes)
docker compose --profile all up -d --build

# Tail logs
docker compose logs -f

# Stop everything
docker compose down
```

Database defaults: `medikit` / `medikit`. Each service uses its own database:
`medikit_users`, `medikit_products`, `medikit_inventory`, `medikit_orders`, `medikit_payments`, `medikit_delivery`, `medikit_prescriptions`, `medikit_health`, `medikit_loyalty`, `medikit_discounts`.

---

## Deploying to Kubernetes

```bash
# From a machine with kubectl pointing at your cluster:
./scripts/deploy-k8s.sh full
```

Or manually:
```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/config/
kubectl apply -f k8s/postgres/ k8s/redis/ k8s/kafka/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/hpa/
kubectl apply -f k8s/ingress/
```

**Important:** Replace the placeholder secrets in `k8s/base/config/medikit-secrets.yaml` (or use External Secrets / Sealed Secrets) before production. Update the image tag in deployments from `:latest` to your released version in a real deployment.

---

## CI/CD Pipeline

`.github/workflows/ci-cd.yml` implements a full pipeline:

```mermaid
graph LR
    A["Push to main"] --> B["Build & Test"]
    B --> C["SonarQube Quality Gate"]
    B --> D["Build Docker Images (14 services)"]
    D --> E["Push to GHCR"]
    E --> F["Deploy to Kubernetes"]
    C --> F
```

- **Build & Test**: runs on every push/PR (`mvn verify`)
- **SonarQube**: static analysis on main
- **Docker**: multi-arch images built with Buildx + GHCR caching, pushed to `ghcr.io/<owner>/medikit/<service>`
- **Deploy**: applies K8s manifests and waits for rollouts

Required secrets: `KUBE_CONFIG`, `SONAR_TOKEN`, `SONAR_HOST_URL`.

---

## API Overview

| Method | Path | Service | Auth |
|--------|------|---------|------|
| POST | `/api/v1/auth/register` | user | Public |
| POST | `/api/v1/auth/login` | user | Public |
| POST | `/api/v1/auth/otp/verify` | user | Public |
| GET | `/api/v1/users/me` | user | Bearer |
| POST | `/api/v1/users/me/addresses` | user | Bearer |
| GET | `/api/v1/products/search?q=` | product | Public |
| GET | `/api/v1/products/trending` | product | Public |
| GET | `/api/v1/products/{id}/alternatives` | product | Public |
| GET | `/api/v1/search/products?q=` | search | Public |
| GET | `/api/v1/search/suggest?q=` | search | Public |
| GET | `/api/v1/cart` | cart | Bearer |
| POST | `/api/v1/cart/items` | cart | Bearer |
| GET | `/api/v1/inventory/stock` | inventory | Public |
| POST | `/api/v1/orders` | order | Bearer |
| GET | `/api/v1/orders/{orderId}` | order | Bearer |
| POST | `/api/v1/payments/initiate` | payment | Bearer |
| GET | `/api/v1/delivery/slots` | delivery | Public |
| GET | `/api/v1/delivery/{orderId}` | delivery | Bearer |
| POST | `/api/v1/prescriptions/upload` | prescription | Bearer |
| POST | `/api/v1/prescriptions/{id}/validate` | prescription | Bearer |
| POST | `/api/v1/health/interactions/check` | health | Public |
| GET | `/api/v1/health/interactions` | health | Public |
| POST | `/api/v1/health/symptoms/analyze` | health | Public |
| GET | `/api/v1/health/conditions` | health | Public |
| POST | `/api/v1/health/prescriptions/analyze` | health | Public |
| POST | `/api/v1/health/assistant/chat` | health | Public |
| GET | `/api/v1/loyalty/balance` | loyalty | Public |
| GET | `/api/v1/loyalty/transactions` | loyalty | Public |
| POST | `/api/v1/loyalty/redeem` | loyalty | Public |
| GET | `/api/v1/loyalty/referral` | loyalty | Public |
| POST | `/api/v1/loyalty/referrals/register` | loyalty | Public |
| POST | `/api/v1/discounts/issue` | discount | Public |
| POST | `/api/v1/discounts/validate` | discount | Public |
| POST | `/api/v1/discounts/redeem` | discount | Public |
| GET | `/api/v1/discounts/my` | discount | Public |
| POST | `/api/v1/campaigns` | discount | Public |
| GET | `/api/v1/campaigns` | discount | Public |
| GET | `/api/v1/campaigns/{id}` | discount | Public |
| GET | `/api/v1/campaigns/{id}/codes` | discount | Public |
| PUT | `/api/v1/users/me/role` | user | Bearer |
| POST | `/api/v1/distributors/register` | distributor | Bearer |
| GET | `/api/v1/distributors/me` | distributor | Bearer |
| GET | `/api/v1/distributors` | distributor | Public |
| GET | `/api/v1/distributors/{id}/catalog` | distributor | Public |
| POST | `/api/v1/distributors/me/catalog` | distributor | Bearer |
| PUT | `/api/v1/distributors/me/catalog/{itemId}` | distributor | Bearer |
| DELETE | `/api/v1/distributors/me/catalog/{itemId}` | distributor | Bearer |
| POST | `/api/v1/distributor-orders` | distributor | Bearer |
| PATCH | `/api/v1/distributor-orders/{id}/status` | distributor | Bearer |
| GET | `/api/v1/distributor-orders` | distributor | Bearer |
| GET | `/api/v1/distributor-orders/distributor` | distributor | Bearer |
| POST | `/api/v1/fulfillments/claim` | distributor | Bearer |
| PATCH | `/api/v1/fulfillments/{id}/status` | distributor | Bearer |
| GET | `/api/v1/fulfillments` | distributor | Bearer |

> **AI Health Intelligence** - `POST /api/v1/health/interactions/check` accepts a list of drug names (brands or salts), resolves them to canonical salts, and returns all known interactions with severity (contraindicated / major / moderate / minor) plus clinical advice. `POST /api/v1/health/symptoms/analyze` maps reported symptoms to probable conditions, ranks them by accumulated confidence, and recommends OTC / prescription remedies with safety flags (including urgent-action alerts for red-flag symptoms). `POST /api/v1/health/prescriptions/analyze` extracts medicine names from OCR'd/typed prescription text, resolves brands to salts, cross-checks against the order items, and flags drug interactions and order discrepancies. `POST /api/v1/health/assistant/chat` accepts a free-text question plus optional context drugs, detects the intent, and returns a plain-English clinical summary (rule-based by default; optional LLM backend gated by `HEALTH_ASSISTANT_LLM_ENABLED` with `USER_HEALTH_LLM_*` env vars).

> **Loyalty** - `GET /api/v1/loyalty/balance` returns the caller's point balance, tier, earn multiplier and next-tier threshold. `GET /api/v1/loyalty/transactions` pages the points ledger. `POST /api/v1/loyalty/redeem` with `{ "points": <n> }` (min 100) mints a `MEDIKIT-` discount code and debits the balance. `GET /api/v1/loyalty/referral` returns the caller's referral code and shareable URL. `POST /api/v1/loyalty/referrals/register` with `{ "referralCode": "REF-XXXX" }` attributes the caller to a referrer, who earns a bonus when the caller's first order is confirmed.

> **Discounts** - `POST /api/v1/discounts/issue` creates a single-use, user-scoped code with a 30-day expiry. `POST /api/v1/discounts/validate` checks a code is active, unexpired and owned by the caller. `POST /api/v1/discounts/redeem` marks it used against an order. `GET /api/v1/discounts/my` pages the caller's issued codes. `order-service` validates and applies the code at checkout. `POST /api/v1/campaigns` creates a promotion campaign and batch-issues up to 10,000 codes (fixed amount or percentage-off, optionally first-order-only); `GET /api/v1/campaigns/{id}/codes` lists a campaign's codes. Percentage codes are applied pro-rata against the subtotal, and first-order-only codes are rejected for returning users.

> **Distributor Portal** - any medical shop can self-upgrade to `DISTRIBUTOR` via `PUT /api/v1/users/me/role`, then register its profile at `POST /api/v1/distributors/register`. `GET /api/v1/distributors` lists active distributors; `GET /api/v1/distributors/{id}/catalog` shows a distributor's wholesale catalog. Shops place B2B supply orders at `POST /api/v1/distributor-orders` (stock deducted on placement, `DB2B-` order number); the distributor moves orders through PENDING → CONFIRMED → SHIPPED → DELIVERED via `PATCH /api/v1/distributor-orders/{id}/status` (cancellation restores stock). For B2C, a distributor claims a customer retail order at `POST /api/v1/fulfillments/claim` (verified against `order-service`) and tracks it CLAIMED → PICKED_UP → IN_TRANSIT → DELIVERED via `PATCH /api/v1/fulfillments/{id}/status`.

Full interactive docs: each service exposes Swagger UI at `/swagger-ui.html`.

---

## Monitoring & Observability

Every service exposes Spring Actuator at `/actuator`:
- `/actuator/health` - liveness/readiness probes for K8s
- `/actuator/metrics` - JVM, HikariCP, HTTP metrics
- `/actuator/prometheus` - Prometheus scrape endpoint

Suggested stack (Phase 6): Prometheus + Grafana, Micrometer Tracing (OpenTelemetry) + Tempo, Loki + Promtail.

---

## Project Structure

```
medikit/
├── pom.xml                          # Parent POM (dependency management)
├── docker-compose.yml               # Local infra + services
├── services/
│   ├── common/                      # Shared library (errors, events, JWT, topics)
│   ├── discovery-server/
│   ├── config-server/
│   ├── api-gateway/
│   ├── user-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── delivery-service/
│   ├── notification-service/
│   ├── prescription-service/
│   ├── search-service/
│   ├── health-service/              # AI health intelligence
│   ├── loyalty-service/             # Points ledger, tiers, redemptions
│   └── discount-service/            # Discount codes, campaigns, redemption
│   └── distributor-service/         # Distributor portal: B2B supply + B2C fulfillment
├── k8s/                             # Kubernetes manifests
│   ├── base/                        # namespace, configmaps, secrets
│   ├── postgres/ redis/ kafka/ elasticsearch/
│   ├── deployments/                 # Deployment + Service per microservice
│   ├── hpa/                         # Autoscaling per service
│   └── ingress/
├── .github/workflows/ci-cd.yml      # CI/CD pipeline
├── docs/
│   └── DEVELOPMENT_GUIDE.md         # Conventions for contributors
└── scripts/
    ├── dev.sh                       # Local dev helper
    └── deploy-k8s.sh                # K8s deployment helper
```

---

## Roadmap

- [x] Phase 0-5: Platform, catalog, commerce, delivery, K8s, CI/CD
- [ ] Phase 6: Scale hardening for 50K concurrent users (9/11 done; service mesh + chaos engineering remain)
- [x] Real payment providers, SMS/email providers, object storage
- [x] Phase 7: Production compliance (audit, retention, DR, pharmacist verification)
- [x] Phase 8: AI Health Intelligence (interaction checker, symptom recommender, prescription analysis, assistant chat API)
- [x] Phase 10: Distributor Portal (B2B wholesale supply + B2C retail fulfillment)
- [x] Phase 11: Delivery Partner Portal (claim pool, role-guarded status updates, live location tracking)
- [ ] Mobile apps (React Native / Flutter) consuming the gateway API
- [ ] Distributor dashboard web app (static HTML/JS against distributor-service APIs)

---

## License

[MIT](LICENSE) © Arijit Rana
