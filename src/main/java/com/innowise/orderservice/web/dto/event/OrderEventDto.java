package com.innowise.orderservice.web.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderEventDto(
        Long orderId,
        UUID userId,
        BigDecimal amount
) {}
