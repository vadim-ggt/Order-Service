package com.innowise.orderservice.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemRequestDto(
        @NotBlank(message = "Item name is required")
        String name,

        @NotNull(message = "Item price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price
) {}