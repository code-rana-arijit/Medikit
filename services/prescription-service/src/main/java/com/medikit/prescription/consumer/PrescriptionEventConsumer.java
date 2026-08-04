package com.medikit.prescription.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionEventConsumer {

    public static final String PRESCRIPTION_VALIDATED = "medikit.prescription.validated";

    private static final Logger log = LoggerFactory.getLogger(PrescriptionEventConsumer.class);

    @KafkaListener(topics = PRESCRIPTION_VALIDATED, groupId = "prescription-service")
    public void onValidated(String message) {
        log.info("Received prescription validated event: {}", message);
    }
}
