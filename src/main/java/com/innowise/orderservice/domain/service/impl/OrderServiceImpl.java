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
    import com.innowise.orderservice.web.client.UserClient;
    import com.innowise.orderservice.web.dto.request.OrderItemRequestDto;
    import com.innowise.orderservice.web.dto.request.OrderRequestDto;
    import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
    import com.innowise.orderservice.web.dto.response.OrderResponseDto;
    import com.innowise.orderservice.web.dto.user.UserInfoDto;
    import jakarta.persistence.EntityNotFoundException;
    import lombok.RequiredArgsConstructor;
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
    @Transactional(readOnly = true)
    public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final ItemRepository itemRepository;
        private final OrderMapper orderMapper;
        private final UserClient userClient;

        @Override
        @Transactional
        public OrderResponseDto createOrder(OrderRequestDto requestDto, UUID userId, String email) {
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

            UserInfoDto userInfo = userClient.getUserByEmail(email);
            return orderMapper.toDtoWithUser(savedOrder, userInfo);
        }

        @Override
        public OrderResponseDto getOrderById(Long id, String email) {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

            UserInfoDto userInfo = userClient.getUserByEmail(email);
            return orderMapper.toDtoWithUser(order, userInfo);
        }


        @Override
        public Page<OrderResponseDto> getFilteredOrders(UUID userId, String email, List<OrderStatus> statuses, LocalDateTime from, LocalDateTime to, Pageable pageable) {

            Specification<Order> spec = Specification.allOf(
                    userId != null
                            ? OrderSpecification.hasUserId(userId)
                            : null,
                    statuses != null && !statuses.isEmpty()
                            ? OrderSpecification.hasStatuses(statuses)
                            : null,
                    from != null || to != null
                            ? OrderSpecification.isCreatedBetween(from, to)
                            : null
            );

            Page<Order> orderPage = orderRepository.findAll(spec, pageable);

            UserInfoDto userInfo = userClient.getUserByEmail(email);

            return orderPage.map(order -> orderMapper.toDtoWithUser(order, userInfo));
        }

        @Override
        @Transactional
        public OrderResponseDto updateOrderStatus(Long id, UpdateOrderStatusDto statusDto, String email) {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

            order.setStatus(statusDto.status());

            Order updatedOrder = orderRepository.save(order);

            UserInfoDto userInfo = userClient.getUserByEmail(email);
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
        public Page<OrderResponseDto> getOrdersByUserId(UUID userId, String email, Pageable pageable) {
            Page<Order> orderPage = orderRepository.findAllByUserId(userId, pageable);

            UserInfoDto userInfo = userClient.getUserByEmail(email);

            return orderPage.map(order -> orderMapper.toDtoWithUser(order, userInfo));
        }

        @Override
        public Page<OrderResponseDto> getOrdersByIds(List<Long> ids, String email, Pageable pageable) {
            Page<Order> orderPage = orderRepository.findAllByIdIn(ids, pageable);

            UserInfoDto userInfo = userClient.getUserByEmail(email);

            return orderPage.map(order -> orderMapper.toDtoWithUser(order, userInfo));
        }
    }
