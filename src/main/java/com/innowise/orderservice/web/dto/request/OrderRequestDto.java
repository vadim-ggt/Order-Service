package com.innowise.orderservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequestDto(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequestDto> items
) {}
