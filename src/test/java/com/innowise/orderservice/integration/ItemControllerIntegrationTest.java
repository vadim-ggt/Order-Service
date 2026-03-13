package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.domain.dao.ItemRepository;
import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ItemControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    @AfterEach
    void cleanup() {
        itemRepository.deleteAll();
    }

    private SimpleGrantedAuthority[] adminAuth() {
        return new SimpleGrantedAuthority[]{new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ADMIN")};
    }

    private SimpleGrantedAuthority[] userAuth() {
        return new SimpleGrantedAuthority[]{new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("USER")};
    }

    private Item createTestItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return itemRepository.save(item);
    }


    @Test
    void test_createItem_Success_AsAdmin() throws Exception {
        ItemRequestDto request = new ItemRequestDto("MacBook Pro", new BigDecimal("2500.00"));

        mockMvc.perform(post("/api/v1/items")
                        .with(jwt().authorities(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("MacBook Pro"))
                .andExpect(jsonPath("$.price").value(2500.00));

        assertEquals(1, itemRepository.count());
    }

    @Test
    void test_createItem_Forbidden_AsUser() throws Exception {
        ItemRequestDto request = new ItemRequestDto("MacBook Pro", new BigDecimal("2500.00"));

        mockMvc.perform(post("/api/v1/items")
                        .with(jwt().authorities(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void test_createItem_BadRequest_ValidationFailed() throws Exception {
        // Отправляем пустую строку и отрицательную цену
        ItemRequestDto request = new ItemRequestDto("", new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/items")
                        .with(jwt().authorities(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void test_getItemById_Success() throws Exception {
        Item savedItem = createTestItem("iPhone", "999.00");

        mockMvc.perform(get("/api/v1/items/{id}", savedItem.getId())
                        .with(jwt().authorities(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"));
    }

    @Test
    void test_getItemById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/items/{id}", 99999L)
                        .with(jwt().authorities(userAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void test_getAllItems_Success() throws Exception {
        createTestItem("Item 1", "10.00");
        createTestItem("Item 2", "20.00");

        mockMvc.perform(get("/api/v1/items?page=0&size=10")
                        .with(jwt().authorities(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }


    @Test
    void test_updateItem_Success_AsAdmin() throws Exception {
        Item savedItem = createTestItem("Old Name", "100.00");
        ItemRequestDto updateRequest = new ItemRequestDto("New Name", new BigDecimal("150.00"));

        mockMvc.perform(put("/api/v1/items/{id}", savedItem.getId())
                        .with(jwt().authorities(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    void test_updateItem_Forbidden_AsUser() throws Exception {
        Item savedItem = createTestItem("Old Name", "100.00");
        ItemRequestDto updateRequest = new ItemRequestDto("New Name", new BigDecimal("150.00"));

        mockMvc.perform(put("/api/v1/items/{id}", savedItem.getId())
                        .with(jwt().authorities(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void test_deleteItem_Success_AsAdmin() throws Exception {
        Item savedItem = createTestItem("To Delete", "50.00");

        mockMvc.perform(delete("/api/v1/items/{id}", savedItem.getId())
                        .with(jwt().authorities(adminAuth())))
                .andExpect(status().isNoContent());

        assertTrue(itemRepository.findById(savedItem.getId()).isEmpty());
    }

    @Test
    void test_deleteItem_Forbidden_AsUser() throws Exception {
        Item savedItem = createTestItem("To Delete", "50.00");

        mockMvc.perform(delete("/api/v1/items/{id}", savedItem.getId())
                        .with(jwt().authorities(userAuth())))
                .andExpect(status().isForbidden());
    }
}