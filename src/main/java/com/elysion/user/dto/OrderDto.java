// src/main/java/com/elysion/user/dto/OrderDto.java
package com.elysion.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Repräsentiert eine einzelne Bestellung in der Historie.
 */
@Data
public class OrderDto {
    /** Eindeutige ID der Bestellung */
    private UUID orderId;

    /** Zeitpunkt der Bestellung */
    private Instant orderDate;

    /** Gesamtbetrag der Bestellung */
    private BigDecimal totalAmount;

    /** Status der Bestellung, z.B. "PENDING", "SHIPPED", "DELIVERED" */
    private String status;

    // ggf. weitere Felder wie List<OrderItemDto> items;
}
