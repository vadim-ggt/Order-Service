package com.innowise.orderservice.web.dto.request;

import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusDto(
        @NotNull(message = "Status cannot be null")
        OrderStatus status
) {}
