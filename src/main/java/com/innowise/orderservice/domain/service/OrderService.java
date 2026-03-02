package com.innowise.orderservice.domain.service;

import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import com.innowise.orderservice.web.dto.request.OrderRequestDto;
import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
import com.innowise.orderservice.web.dto.response.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto requestDto, UUID userId, String email);

    OrderResponseDto getOrderById(Long id, String email);

    Page<OrderResponseDto> getFilteredOrders(UUID userId,
                                             String email,
                                             List<OrderStatus> statuses,
                                             LocalDateTime from,
                                             LocalDateTime to,
                                             Pageable pageable);

    OrderResponseDto updateOrderStatus(Long id, UpdateOrderStatusDto statusDto, String email);

    void deleteOrder(Long id);

    Page<OrderResponseDto> getOrdersByUserId(UUID userId, String email, Pageable pageable);

    Page<OrderResponseDto> getOrdersByIds(List<Long> ids, String email, Pageable pageable);

}
