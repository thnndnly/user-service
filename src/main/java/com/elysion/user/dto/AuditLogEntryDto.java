package com.elysion.user.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class AuditLogEntryDto {
    private String action;
    private Instant timestamp;
    private Map<String, Object> metadata;
}