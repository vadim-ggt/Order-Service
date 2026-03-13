package com.innowise.orderservice.web.dto.response;

public record OrderItemResponseDto(
        Long id,
        ItemDto item,
        Integer quantity
) {}
