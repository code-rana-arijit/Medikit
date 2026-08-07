# Medikit Data Protection & Retention Policy

Applies to all personal and health data processed by the Medikit platform.
This policy operationalizes GDPR (data minimization, purpose limitation, the
right to erasure/access) and HIPAA (privacy rule, audit controls, minimum
necessary) requirements.

## 1. Data Classification

| Category             | Examples                                   | Handling                                        |
|----------------------|--------------------------------------------|-------------------------------------------------|
| PII                 | name, email, phone, address                | Encrypt at rest; audit every read/write         |
| PHI (prescriptions) | image, patient name/age, diagnosis, doctor | Encrypt at rest; access-logged; short retention |
| Payment data        | amount, currency, provider ref             | Never stored raw; gateway tokens only           |
| Operational         | logs, metrics, events                      | Audit events immutable, 180-day retention       |

## 2. Audit Controls (HIPAA §164.312(b) + GDPR Art. 30)

Every access to PHI/PII emits an immutable audit event to the Kafka topic
`medikit.audit.events` via `AuditService`:

- Action (`payment.captured`, `prescription.accessed`, `admin.review`, ...)
- Actor id + role
- Resource type + id
- Timestamp (UTC ISO-8601)
- Correlation id for trace linkage

Audit events never contain raw secrets, card numbers, or full PHI bodies.
The topic is write-only for services; reads are restricted to compliance
tooling.

## 3. Retention Schedule

| Dataset                | Retention | Enforcement                                     |
|------------------------|-----------|-------------------------------------------------|
| Prescriptions          | 30 days   | `ExpiryScheduler` marks APPROVED -> EXPIRED     |
| Audit events           | 180 days  | Kafka tiered storage / S3 lifecycle rule        |
| Order records          | 36 months | DB archival job (disaster recovery docs)        |
| Chat / support         | 12 months | Downstream CRM policy                           |
| Inactive accounts      | 24 months | De-identification job                           |

After the retention window, records are purged or de-identified, not merely
soft-deleted.

## 4. Data Subject Requests (GDPR Art. 15-17)

`AuditService.recordSubjectRequest(actor, subjectId, type)` records
ACCESS and ERASURE requests. Compliance handlers:

- **Access**: export the user's PII/PHI as a machine-readable bundle.
- **Erasure**: anonymize identity columns (keep order/payment aggregates),
  delete prescription images from S3, revoke tokens/sessions.
- **Rectification**: profile update flow, logged as `pii.updated`.

## 5. Engineering Requirements

- PHI uploads stored server-side via S3 with private ACLs; URLs are signed and
  short-lived.
- Passwords hashed (BCrypt), JWTs short-lived with refresh rotation.
- No PHI/PII in application logs; log redaction filters enforced at gateway.
- Encryption in transit (mTLS/Istio) and at rest (volume encryption, KMS).

## 6. Operational Playbook

| Trigger                 | Owner       | Actions                                           |
|-------------------------|-------------|---------------------------------------------------|
| Data breach suspicion   | Security    | Freeze account, snapshot audit topic, notify      |
| DSR received            | Privacy     | Verify identity, run access/erasure workflow      |
| Regulatory audit        | Compliance  | Export audit topic to cold storage, generate report |
| Retention enforcement   | DataEng     | Verify purge jobs, spot-check de-identification   |
