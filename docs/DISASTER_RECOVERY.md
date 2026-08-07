# Medikit Disaster Recovery Plan

Targets: **RPO ≤ 15 minutes**, **RTO ≤ 2 hours** for core commerce flows,
**RTO ≤ 24 hours** for analytics/history.

## 1. Failure Domains & Mitigation

| Domain       | Failure                          | Mitigation                                       |
|--------------|----------------------------------|--------------------------------------------------|
| Stateless    | Pod / node loss                  | Deployments + HPA, PDBs, multiple AZs            |
| Stateful     | Postgres primary loss            | Read replicas + PgBouncer, WAL archiving         |
| Cache        | Redis shard loss                 | Redis Cluster 3x replication                     |
| Messaging    | Kafka broker loss                | Multi-broker, replication.factor, ISR            |
| Object store | S3 region outage                 | Cross-region replication bucket policy           |
| Region       | Whole-region outage              | DR site with warm Postgres replica + backup restore |

## 2. RPO/RTO Matrix

| Component   | Backup mechanism                  | Frequency | RPO    | RTO          |
|-------------|-----------------------------------|-----------|--------|--------------|
| Postgres    | `pg_dump` custom format (CronJob) | Daily     | 24h    | 2h (restore) |
| Postgres WAL| WAL archiving to S3               | Continuous| 5-15m  | 2h           |
| S3 objects  | Versioning + cross-region rule    | Continuous| 0      | minutes      |
| Kafka       | Log retention (7d) + offsets      | Built-in  | 7d     | 1h           |
| Config/Secrets | GitOps manifest (committed)    | On change | N/A    | minutes      |

## 3. Backup Operations

- Daily logical backup: `k8s/backup/postgres-backup.yaml` CronJob at 02:00 UTC.
  Two-stage job: `postgres:16-alpine` runs `pg_dump --format=custom`, the
  `amazon/aws-cli` sidecar uploads to `medikit-backups/postgres/`.
- Restore drill monthly: restore the latest dump into a scratch Postgres,
  validate row counts per service schema, then promote to primary.
- Backup bucket uses versioning and a lifecycle rule (30-day cold storage).

## 4. Restore Runbook (RTO ≤ 2h)

### 4.1 Stateless platform
```
kubectl apply -k k8s/                    # recreates all stateless workloads
```
Eureka + config server boot, services re-register, ingress routes traffic.

### 4.2 Postgres primary
```bash
# 1. Spin up fresh Postgres
kubectl apply -f k8s/postgres/postgres.yaml

# 2. Download latest dump and restore
aws s3 cp s3://medikit-backups/postgres/latest.dump /tmp/restore.dump
pg_restore -h postgres -U medikit -d medikit --clean --if-exists /tmp/restore.dump

# 3. Point PgBouncer at the new primary (DNS update), verify health checks
```

### 4.3 Redis / Kafka / Elasticsearch
- Redis Cluster: `kubectl scale` replicas; cluster heals via gossip. Warm cache
  rebuilds from DB/ES on demand.
- Kafka: brokers rejoin; log segments on persistent volumes are retained.
- Elasticsearch: single-node index; re-index from `medikit.product.updated`
  topic or run `SearchIndexService.bulkIndex` backfill.

## 5. Region Failover

1. Promote DR-site Postgres replica (WAL-shipped) to primary; update PgBouncer.
2. Switch gateway DNS / global load balancer to DR site ingress.
3. Re-point Kafka consumer groups to DR cluster if the primary region's
   cluster is unreachable (accepts at-most-once reprocessing; idempotent
   consumers make this safe).
4. Validate: health checks green, synthetic smoke test passes, audit topic
   receiving events.

## 6. Testing & Ownership

| Activity              | Cadence    | Owner       |
|-----------------------|------------|-------------|
| Backup restore drill  | Monthly    | DataEng     |
| Failover simulation   | Quarterly  | SRE         |
| Chaos Mesh test       | Per release| SRE/QA      |
| Secret rotation drill | Quarterly  | Security    |
