package com.medikit.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String id;
    private String type;
    private String source;
    private String payload;
    private Instant occurredAt;

    public static DomainEvent of(String type, String source, Object payload) {
        return DomainEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .source(source)
                .payload(payload != null ? toJson(payload) : null)
                .occurredAt(Instant.now())
                .build();
    }

    private static String toJson(Object payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            return payload.toString();
        }
    }
}
