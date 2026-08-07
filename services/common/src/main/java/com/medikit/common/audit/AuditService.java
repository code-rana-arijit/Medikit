package com.medikit.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GDPR/HIPAA-compliant audit logging.
 * <p>
 * Every sensitive operation (payment capture, prescription access, PII read,
 * admin review) records an immutable audit event to the
 * {@code medikit.audit.events} Kafka topic. Events carry an actor, action,
 * resource, and a correlation id, and never contain raw secrets. A retention
 * policy (Kafka tiered storage / S3 lifecycle) removes events older than the
 * configured period, supporting data-minimization requirements.
 * </p>
 */
@Service
public class AuditService {

    public static final String AUDIT_TOPIC = "medikit.audit.events";

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AuditService(EventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public void record(AuditAction action, String actorId, String actorRole, String resourceType,
                       String resourceId, Map<String, Object> details) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("version", 1);
        event.put("timestamp", Instant.now().toString());
        event.put("action", action.code());
        event.put("actorId", actorId);
        event.put("actorRole", actorRole);
        event.put("resourceType", resourceType);
        event.put("resourceId", resourceId);
        if (details != null && !details.isEmpty()) {
            event.put("details", details);
        }
        try {
            String key = resourceType + ":" + (resourceId == null ? "?" : resourceId);
            eventPublisher.publish(AUDIT_TOPIC, key, event);
        } catch (Exception e) {
            log.error("Failed to publish audit event for action {} resource {}/{}",
                    action, resourceType, resourceId, e);
        }
    }

    /**
     * Record a GDPR data-subject-request event (access / erasure).
     */
    public void recordSubjectRequest(String actorId, String subjectId, String requestType) {
        record(AuditAction.DATA_SUBJECT_REQUEST, actorId, "SYSTEM", "user", subjectId,
                Map.of("requestType", requestType));
    }

    public enum AuditAction {
        PAYMENT_CAPTURED("payment.captured"),
        PAYMENT_REFUNDED("payment.refunded"),
        PRESCRIPTION_ACCESSED("prescription.accessed"),
        PII_READ("pii.read"),
        PII_UPDATED("pii.updated"),
        ADMIN_REVIEW("admin.review"),
        PHARMACIST_VERIFIED("pharmacist.verified"),
        DATA_SUBJECT_REQUEST("gdpr.data_subject_request"),
        LOGIN("auth.login"),
        LOGOUT("auth.logout");

        private final String code;

        AuditAction(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
