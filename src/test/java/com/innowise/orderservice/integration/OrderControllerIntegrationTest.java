package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.domain.dao.ItemRepository;
import com.innowise.orderservice.domain.dao.OrderRepository;
import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.domain.entity.Order;
import com.innowise.orderservice.domain.entity.OrderItem;
import com.innowise.orderservice.domain.entity.enums.OrderStatus;
import com.innowise.orderservice.web.client.provider.UserProvider;
import com.innowise.orderservice.web.dto.request.OrderItemRequestDto;
import com.innowise.orderservice.web.dto.request.OrderRequestDto;
import com.innowise.orderservice.web.dto.request.UpdateOrderStatusDto;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @MockitoBean
    private UserProvider userProvider;

    private Item testItem;
    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setup() {
        testItem = itemRepository.save(Item.builder()
                .name("PlayStation 5")
                .price(new BigDecimal("500.00"))
                .build());

        UserInfoDto mockUserInfo = new UserInfoDto(TEST_USER_ID, "John", "Doe", TEST_EMAIL, LocalDate.of(1990, 1, 1));
        when(userProvider.getStrictUserInfo(anyString())).thenReturn(mockUserInfo);
        when(userProvider.getUserInfoForRead(anyString())).thenReturn(mockUserInfo);
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }


    private RequestPostProcessor userJwt(UUID userId, String email) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("USER"))
                .jwt(jwt -> jwt.subject(userId.toString()).claim("email", email));
    }

    private RequestPostProcessor adminJwt(String email) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ADMIN"))
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("email", email));
    }

    private Order createTestOrderInDb(UUID userId, OrderStatus status) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalPrice(testItem.getPrice());

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(testItem);
        orderItem.setQuantity(1);
        order.addOrderItem(orderItem);

        return orderRepository.save(order);
    }



    @Test
    void test_createOrder_Success_AsUser() throws Exception {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(testItem.getId(), 2);
        OrderRequestDto request = new OrderRequestDto(List.of(itemRequest));

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(1000.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));

        assertEquals(1, orderRepository.count());
    }

    @Test
    void test_createOrder_BadRequest_EmptyItems() throws Exception {
        OrderRequestDto request = new OrderRequestDto(List.of());

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void test_getOrderById_Success() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);

        mockMvc.perform(get("/api/orders/{id}", savedOrder.getId())
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()));
    }



    @Test
    void test_getMyOrders_Success() throws Exception {
        createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);
        createTestOrderInDb(TEST_USER_ID, OrderStatus.SHIPPING);

        mockMvc.perform(get("/api/orders/my?page=0&size=10")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }


    @Test
    void test_searchOrders_Success_AsAdmin() throws Exception {
        createTestOrderInDb(TEST_USER_ID, OrderStatus.DELIVERED);

        mockMvc.perform(get("/api/orders/search?statuses=DELIVERED")
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void test_searchOrders_Forbidden_AsUser() throws Exception {
        mockMvc.perform(get("/api/orders/search")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL)))
                .andExpect(status().isForbidden());
    }


    @Test
    void test_updateOrderStatus_Success_AsAdmin() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.SHIPPING);

        mockMvc.perform(put("/api/orders/{id}/status", savedOrder.getId())
                        .with(adminJwt("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPING"));
    }

    @Test
    void test_updateOrderStatus_Forbidden_AsUser() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.SHIPPING);

        mockMvc.perform(put("/api/orders/{id}/status", savedOrder.getId())
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }


    @Test
    void test_deleteOrder_Success_AsAdmin() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);

        mockMvc.perform(delete("/api/orders/{id}", savedOrder.getId())
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isNoContent());

        assertTrue(orderRepository.findById(savedOrder.getId()).isEmpty());
    }


    @Test
    void test_createOrder_WithNegativeQuantity_ReturnsBadRequest() throws Exception {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(testItem.getId(), -1);
        OrderRequestDto request = new OrderRequestDto(List.of(itemRequest));

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test_createOrder_WithNullQuantity_ReturnsBadRequest() throws Exception {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(testItem.getId(), null);
        OrderRequestDto request = new OrderRequestDto(List.of(itemRequest));

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void test_getOrderById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999L)
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void test_updateOrderStatus_NonExistentOrder_Returns404() throws Exception {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto(OrderStatus.SHIPPING);

        mockMvc.perform(put("/api/orders/{id}/status", 99999L)
                        .with(adminJwt("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void test_createOrder_WithNonExistentItem_Returns404() throws Exception {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(99999L, 2);
        OrderRequestDto request = new OrderRequestDto(List.of(itemRequest));

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }


    @Test
    void test_getOrderById_AsDifferentUser_ReturnsForbidden() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);
        UUID otherUserId = UUID.randomUUID(); // Другой пользователь

        mockMvc.perform(get("/api/orders/{id}", savedOrder.getId())
                        .with(userJwt(otherUserId, "other@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void test_getOrderById_AsAdmin_DifferentUser_Success() throws Exception {
        Order savedOrder = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);

        // Админ должен иметь право смотреть чужие заказы
        mockMvc.perform(get("/api/orders/{id}", savedOrder.getId())
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()));
    }


    @Test
    void test_searchOrders_WithDateRange_Success() throws Exception {
        createTestOrderInDb(TEST_USER_ID, OrderStatus.DELIVERED);

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/api/orders/search")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void test_searchOrders_ByUserIdAndStatus_Success() throws Exception {
        createTestOrderInDb(TEST_USER_ID, OrderStatus.DELIVERED);

        mockMvc.perform(get("/api/orders/search")
                        .param("userId", TEST_USER_ID.toString())
                        .param("statuses", "DELIVERED", "CREATED")
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }


    @Test
    void test_getOrdersByIds_Success_AsAdmin() throws Exception {
        Order order1 = createTestOrderInDb(TEST_USER_ID, OrderStatus.CREATED);
        Order order2 = createTestOrderInDb(TEST_USER_ID, OrderStatus.SHIPPING);

        mockMvc.perform(get("/api/orders/by-ids")
                        .param("ids", order1.getId().toString(), order2.getId().toString())
                        .with(adminJwt("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void test_getOrdersByIds_Forbidden_AsUser() throws Exception {
        mockMvc.perform(get("/api/orders/by-ids")
                        .param("ids", "1", "2")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL)))
                .andExpect(status().isForbidden());
    }


    @Test
    void test_getMyOrders_EmptyResult_ReturnsEmptyPage() throws Exception {
        // Заказов нет, проверяем что просто пустой список, а не ошибка
        mockMvc.perform(get("/api/orders/my")
                        .with(userJwt(TEST_USER_ID, TEST_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
