# Medikit Capacity Model

Target: support **50,000 concurrent users** during peak with p95 < 400ms and
error rate < 0.5%. This document records the per-service capacity assumptions,
sizing math, and the verification plan used during Phase 6 hardening.

## 1. Traffic Profile

Assumed traffic mix at peak (an online pharmacy shopping journey):

| Activity            | Share of requests | Peak RPS (50K users) |
|---------------------|-------------------|----------------------|
| Catalog browse      | 40%               | 300                  |
| Search              | 20%               | 150                  |
| Product detail view | 20%               | 150                  |
| Cart / checkout     | 15%               | 50                   |
| Orders / payments   | 5%                | 15                   |

Total gateway throughput target: **~700 RPS**, sustained for minutes.

## 2. Per-Service Capacity & Sizing

### api-gateway (3 replicas)
- Stateless, Spring Cloud Gateway + Redis rate limiter (composite user+IP token buckets).
- Handles ~700 RPS; each replica targets 250 RPS.
- 2 CPU / 2Gi each. HPA scale-out 3–8.

### user-service (3 replicas)
- JWT login/refresh + user profile. Read-heavy after login.
- DB read replicas absorb auth/profile reads. 1 CPU / 1Gi each.

### product-service (2 replicas)
- Catalog reads dominated by Caffeine L1 cache (10K entries, 30s TTL) + Redis L2.
- Cache hit rate target > 90% at peak; un-cached requests hit Postgres replica.
- 1 CPU / 1Gi each. HPA 2–10.

### search-service (2 replicas)
- Redis inverted-index fallback or Elasticsearch engine (enabled in K8s).
- 6 Kafka consumer threads per pod for indexing; p95 search < 100ms.
- 2 CPU / 2Gi each. HPA 2–8.

### cart-service (3 replicas)
- In-memory cart with Redis persistence; each add = 1 write to Redis cluster.
- 1 CPU / 1Gi each. HPA 3–10.

### order-service (2 replicas)
- Saga coordinator: order → inventory → payment → delivery over Kafka.
- Writes to Postgres primary via PgBouncer pool. 2 CPU / 2Gi each. HPA 2–8.

### payment-service / delivery-service / notification-service (2-3 replicas)
- Event consumers; async, low request surface. 1 CPU / 1Gi each.

### prescription-service (2 replicas)
- Moderate write throughput; schema registry on Kafka. 1 CPU / 1Gi each.

## 3. Infrastructure Sizing

### PostgreSQL primary + replicas
- Primary: 4 CPU / 8Gi, PgBouncer pools transactions.
- 2 read replicas absorb all read traffic. PgBouncer max_client_conn 2000,
  pool_mode=transaction, default_pool_size 50.
- PgBouncer replica count 2 behind Service for HA.

### Redis Cluster
- 6 shards × 3 replicas (18 nodes) covers cart/session/lock keyspace.
- Gateway rate-limit keys share the cluster.

### Kafka
- Topics at 12 partitions; producer acks=all for order-critical topics,
  compression=snappy. Consumer groups per service with concurrency 6.

### Elasticsearch
- Single node 8.13.4 for search; index `medikit-products`.

## 4. Bottleneck Budget (700 RPS envelope)

| Layer                | Assumed capacity | Margin |
|----------------------|------------------|--------|
| Gateway (3 pods)     | 750 RPS          | 1.1x   |
| Postgres primary     | 1,000 TPS writes | 1.4x   |
| Postgres replicas (2)| 2,000 TPS reads  | 2.9x   |
| Redis cluster        | > 50K ops/s      | 3x+    |
| Kafka (12 partitions)| > 10K msg/s      | 10x+   |
| Elasticsearch        | 500 QPS          | 3x     |

## 5. Verification Plan (k6)

| Test             | Script                | Command                                                       |
|------------------|-----------------------|---------------------------------------------------------------|
| Smoke            | `k6/smoke.js`         | `k6 run -e BASE_URL=<gw> load-testing/k6/smoke.js`            |
| Peak / soak      | `k6/peak.js`          | `k6 run -e BASE_URL=<gw> -e PEAK_VUS=1000 load-testing/k6/peak.js` |

Run against the staged K8s cluster while Grafana observes Prometheus
(+Tempo traces +Loki logs). Success = thresholds pass with HPAs not pinned
at max and no consumer-lag growth.
