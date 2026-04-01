package com.innowise.orderservice.web.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentEventDto(
        String paymentId,
        Long orderId,
        UUID userId,
        String status,
        BigDecimal amount
) {}
