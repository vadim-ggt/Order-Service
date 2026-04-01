        package com.innowise.orderservice.domain.service.impl;

        import com.innowise.orderservice.domain.dao.ItemRepository;
        import com.innowise.orderservice.domain.dao.OrderRepository;
        import com.innowise.orderservice.domain.entity.Item;
        import com.innowise.orderservice.domain.entity.Order;
        import com.innowise.orderservice.domain.entity.OrderItem;
        import com.innowise.orderservice.domain.entity.enums.OrderStatus;
        import com.innowise.orderservice.domain.exception.ItemNotFoundException;
        import com.innowise.orderservice.domain.exception.OrderNotFoundException;
        import com.innowise.orderservice.domain.mapper.order.OrderMapper;
        import com.innowise.orderservice.domain.service.OrderService;
        import com.innowise.orderservice.domain.specification.OrderSpecification;
        import com.innowise.orderservice.kafka.OrderProducer;
        import com.innowise.orderservice.web.client.provider.UserProvider;
        import com.innowise.orderservice.web.dto.event.OrderEventDto;
        import com.innowise.orderservice.web.dto.request.OrderItemRequestDto;
        import com.innowise.orderservice.web.dto.request.OrderRequestDto;
        import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
        import com.innowise.orderservice.web.dto.response.OrderResponseDto;
        import com.innowise.orderservice.web.dto.user.UserInfoDto;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.springframework.data.domain.Page;
        import org.springframework.data.domain.Pageable;
        import org.springframework.data.jpa.domain.Specification;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;

        import java.math.BigDecimal;
        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.UUID;

        @Service
        @RequiredArgsConstructor
        @Slf4j
        @Transactional(readOnly = true)
        public class OrderServiceImpl implements OrderService {

            private final OrderRepository orderRepository;
            private final ItemRepository itemRepository;
            private final OrderMapper orderMapper;
            private final OrderProducer orderProducer;
            private final UserProvider userProvider;

            @Override
            @Transactional
            public OrderResponseDto createOrder(OrderRequestDto requestDto, UUID userId) {
                Order order = new Order();
                order.setUserId(userId);
                order.setStatus(OrderStatus.CREATED);

                BigDecimal totalPrice = BigDecimal.ZERO;

                for (OrderItemRequestDto itemRequest : requestDto.items()) {
                    Item item = itemRepository.findById(itemRequest.itemId())
                            .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + itemRequest.itemId()));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setItem(item);
                    orderItem.setQuantity(itemRequest.quantity());
                    order.addOrderItem(orderItem);

                    totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
                }

                order.setTotalPrice(totalPrice);
                Order savedOrder = orderRepository.save(order);
                OrderEventDto eventDto = new OrderEventDto(
                        savedOrder.getId(),
                        userId,
                        savedOrder.getTotalPrice()
                );
                orderProducer.sendOrderCreatedEvent(eventDto);

                UserInfoDto userInfo = userProvider.getStrictUserInfoById(userId);
                return orderMapper.toDtoWithUser(savedOrder, userInfo);
            }

            @Override
            public OrderResponseDto getOrderById(Long id) {
                Order order = orderRepository.findById(id)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

                UserInfoDto userInfo = userProvider.getUserInfoByIdForRead(order.getUserId());
                return orderMapper.toDtoWithUser(order, userInfo);
            }


            @Override
            public Page<OrderResponseDto> getFilteredOrders(UUID userId, List<OrderStatus> statuses, LocalDateTime from, LocalDateTime to, Pageable pageable) {
                Specification<Order> spec = Specification.allOf(
                        userId != null ? OrderSpecification.hasUserId(userId) : null,
                        statuses != null && !statuses.isEmpty() ? OrderSpecification.hasStatuses(statuses) : null,
                        from != null || to != null ? OrderSpecification.isCreatedBetween(from, to) : null
                );

                Page<Order> orderPage = orderRepository.findAll(spec, pageable);

                return orderPage.map(order -> {
                    UserInfoDto userInfo = userProvider.getUserInfoByIdForRead(order.getUserId());
                    return orderMapper.toDtoWithUser(order, userInfo);
                });
            }

            @Override
            @Transactional
            public OrderResponseDto updateOrderStatus(Long id, UpdateOrderStatusDto statusDto) {
                Order order = orderRepository.findById(id)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

                order.setStatus(statusDto.status());
                Order updatedOrder = orderRepository.save(order);

                // Берем ID владельца из обновленного заказа
                UserInfoDto userInfo = userProvider.getStrictUserInfoById(updatedOrder.getUserId());
                return orderMapper.toDtoWithUser(updatedOrder, userInfo);
            }


            @Override
            @Transactional
            public void deleteOrder(Long id) {
                if (!orderRepository.existsById(id)) {
                    throw new OrderNotFoundException("Order not found: " + id);
                }
                orderRepository.deleteById(id);
            }

            @Override
            public Page<OrderResponseDto> getOrdersByUserId(UUID userId, Pageable pageable) {
                Page<Order> orderPage = orderRepository.findAllByUserId(userId, pageable);

                UserInfoDto userInfo = userProvider.getUserInfoByIdForRead(userId);

                return orderPage.map(order -> orderMapper.toDtoWithUser(order, userInfo));
            }

            @Override
            public Page<OrderResponseDto> getOrdersByIds(List<Long> ids, Pageable pageable) {
                Page<Order> orderPage = orderRepository.findAllByIdIn(ids, pageable);

                return orderPage.map(order -> {
                    UserInfoDto userInfo = userProvider.getUserInfoByIdForRead(order.getUserId());
                    return orderMapper.toDtoWithUser(order, userInfo);
                });
            }

            @Override
            @Transactional
            public void handlePaymentEvent(Long orderId, String paymentStatus) {
                log.info("Processing payment event for order: {}, status: {}", orderId, paymentStatus);

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

                if ("SUCCESS".equalsIgnoreCase(paymentStatus) || "PAID".equalsIgnoreCase(paymentStatus)) {
                    order.setStatus(OrderStatus.PAID);
                } else if ("FAIL".equalsIgnoreCase(paymentStatus) || "FAILED".equalsIgnoreCase(paymentStatus)) {
                    order.setStatus(OrderStatus.FAILED);
                }

                orderRepository.save(order);
            }
        }
