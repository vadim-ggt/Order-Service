package com.innowise.orderservice.web.controller;

import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import com.innowise.orderservice.domain.service.OrderService;
import com.innowise.orderservice.security.annotation.IsAdmin;
import com.innowise.orderservice.security.annotation.IsOwnerOrAdmin;
import com.innowise.orderservice.security.annotation.IsUserOrAdmin;
import com.innowise.orderservice.web.dto.request.OrderRequestDto;
import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
import com.innowise.orderservice.web.dto.response.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @PostMapping
    @IsUserOrAdmin
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto requestDto,
            JwtAuthenticationToken jwtToken) {

        UUID userId = UUID.fromString(jwtToken.getToken().getSubject());

        OrderResponseDto createdOrder = orderService.createOrder(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    @IsOwnerOrAdmin
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/my")
    @IsUserOrAdmin
    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(
            Pageable pageable,
            JwtAuthenticationToken jwtToken) {

        UUID userId = UUID.fromString(jwtToken.getToken().getSubject());

        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
    }

    @GetMapping("/search")
    @IsAdmin
    public ResponseEntity<Page<OrderResponseDto>> searchOrders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.getFilteredOrders(userId, statuses, from, to, pageable));
    }

    @GetMapping("/by-ids")
    @IsAdmin
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByIds(
            @RequestParam("ids") List<Long> ids,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.getOrdersByIds(ids, pageable));
    }

    @PutMapping("/{id}/status")
    @IsAdmin
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusDto statusDto) {

        return ResponseEntity.ok(orderService.updateOrderStatus(id, statusDto));
    }

    @DeleteMapping("/{id}")
    @IsAdmin
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

}
