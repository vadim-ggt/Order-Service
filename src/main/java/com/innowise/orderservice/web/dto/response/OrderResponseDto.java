package com.innowise.orderservice.web.dto.response;

import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import com.innowise.orderservice.web.dto.user.UserInfoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResponseDto> items,
        UserInfoDto user
) {}
