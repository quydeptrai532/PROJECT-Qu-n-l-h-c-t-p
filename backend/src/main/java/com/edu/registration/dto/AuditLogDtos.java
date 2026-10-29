package com.edu.registration.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public final class AuditLogDtos {

    private AuditLogDtos() {
    }

    public record AuditLogDto(Long id, Long actorUserId, String actorUsername, String actorRole,
                              String action, String entityType, String entityId, LocalDateTime occurredAt,
                              String result, String correlationId, JsonNode details) {
    }
}
