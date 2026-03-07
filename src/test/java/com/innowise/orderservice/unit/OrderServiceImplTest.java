package com.innowise.orderservice.unit;

import com.innowise.orderservice.domain.dao.ItemRepository;
import com.innowise.orderservice.domain.dao.OrderRepository;
import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.domain.entity.Order;
import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import com.innowise.orderservice.domain.exception.ItemNotFoundException;
import com.innowise.orderservice.domain.exception.OrderNotFoundException;
import com.innowise.orderservice.domain.mapper.order.OrderMapper;
import com.innowise.orderservice.domain.service.impl.OrderServiceImpl;
import com.innowise.orderservice.web.client.provider.UserProvider;
import com.innowise.orderservice.web.dto.request.OrderItemRequestDto;
import com.innowise.orderservice.web.dto.request.OrderRequestDto;
import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
import com.innowise.orderservice.web.dto.response.OrderResponseDto;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserProvider userProvider;

    @InjectMocks
    private OrderServiceImpl orderService;

    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final String TEST_EMAIL = "test@example.com";
    private final Long TEST_ORDER_ID = 100L;

    private Item testItem;
    private Order testOrder;
    private UserInfoDto testUserInfo;
    private OrderResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        testItem = new Item(1L, "Laptop", new BigDecimal("1000.00"));

        testOrder = new Order();
        testOrder.setId(TEST_ORDER_ID);
        testOrder.setUserId(TEST_USER_ID);
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setTotalPrice(new BigDecimal("2000.00"));

        testUserInfo = new UserInfoDto(TEST_USER_ID, "John", "Doe", TEST_EMAIL, LocalDate.of(1990, 1, 1));

        testResponseDto = new OrderResponseDto(
                TEST_ORDER_ID, OrderStatus.CREATED, new BigDecimal("2000.00"),
                null, List.of(), testUserInfo
        );
    }



    @Test
    void createOrder_Success() {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(1L, 2);
        OrderRequestDto requestDto = new OrderRequestDto(List.of(itemRequest));

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userProvider.getStrictUserInfo(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        OrderResponseDto result = orderService.createOrder(requestDto, TEST_USER_ID, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.id());
        assertEquals(OrderStatus.CREATED, result.status());

        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userProvider, times(1)).getStrictUserInfo(TEST_EMAIL);
    }

    @Test
    void createOrder_ThrowsItemNotFoundException() {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(99L, 1);
        OrderRequestDto requestDto = new OrderRequestDto(List.of(itemRequest));

        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        ItemNotFoundException exception = assertThrows(ItemNotFoundException.class, () ->
                orderService.createOrder(requestDto, TEST_USER_ID, TEST_EMAIL)
        );

        assertEquals("Item not found with id: 99", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }


    @Test
    void getOrderById_Success() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        OrderResponseDto result = orderService.getOrderById(TEST_ORDER_ID, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.id());
    }

    @Test
    void getOrderById_ThrowsOrderNotFoundException() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                orderService.getOrderById(TEST_ORDER_ID, TEST_EMAIL)
        );
        verify(userProvider, never()).getUserInfoForRead(anyString());
    }


    @Test
    @SuppressWarnings("unchecked")
    void getFilteredOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getFilteredOrders(
                TEST_USER_ID, TEST_EMAIL, List.of(OrderStatus.CREATED), null, null, pageable
        );

        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }


    @Test
    void updateOrderStatus_Success() {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.SHIPPING);

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(userProvider.getStrictUserInfo(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        OrderResponseDto result = orderService.updateOrderStatus(TEST_ORDER_ID, updateDto, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPING, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }



    @Test
    void deleteOrder_Success() {
        when(orderRepository.existsById(TEST_ORDER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.deleteOrder(TEST_ORDER_ID));

        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
    }

    @Test
    void deleteOrder_ThrowsOrderNotFoundException() {
        when(orderRepository.existsById(TEST_ORDER_ID)).thenReturn(false);

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(TEST_ORDER_ID));

        verify(orderRepository, never()).deleteById(anyLong());
    }



    @Test
    void getOrdersByUserId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAllByUserId(TEST_USER_ID, pageable)).thenReturn(orderPage);
        when(userProvider.getStrictUserInfo(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getOrdersByUserId(TEST_USER_ID, TEST_EMAIL, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrdersByIds_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(TEST_ORDER_ID);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAllByIdIn(ids, pageable)).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getOrdersByIds(ids, TEST_EMAIL, pageable);

        assertEquals(1, result.getTotalElements());
    }


    @Test
    @SuppressWarnings("unchecked")
    void getFilteredOrders_AllFiltersProvided_CoversTrueBranches() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        // 1. Все параметры переданы (userId != null, statuses не пуст, from != null)
        Page<OrderResponseDto> result = orderService.getFilteredOrders(
                TEST_USER_ID,
                TEST_EMAIL,
                List.of(OrderStatus.CREATED),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                pageable
        );

        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredOrders_AllFiltersNull_CoversFalseBranches() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getFilteredOrders(
                null,
                TEST_EMAIL,
                null,
                null,
                null,
                pageable
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredOrders_EmptyStatusesAndOnlyToDate_CoversMixedBranches() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getFilteredOrders(
                null,
                TEST_EMAIL,
                List.of(),
                null,
                LocalDateTime.now(),
                pageable
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredOrders_OnlyFromDate_CoversDateOrBranch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userProvider.getUserInfoForRead(TEST_EMAIL)).thenReturn(testUserInfo);
        when(orderMapper.toDtoWithUser(testOrder, testUserInfo)).thenReturn(testResponseDto);

        Page<OrderResponseDto> result = orderService.getFilteredOrders(
                null,
                TEST_EMAIL,
                null,
                LocalDateTime.now(),
                null,
                pageable
        );

        assertEquals(1, result.getTotalElements());
    }

}
