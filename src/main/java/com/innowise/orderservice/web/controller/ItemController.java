package com.innowise.orderservice.web.controller;

import com.innowise.orderservice.domain.service.ItemService;
import com.innowise.orderservice.security.annotation.IsAdmin;
import com.innowise.orderservice.security.annotation.IsUserOrAdmin;
import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import com.innowise.orderservice.web.dto.response.ItemDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @IsAdmin
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemRequestDto requestDto) {
        ItemDto createdItem = itemService.createItem(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @GetMapping("/{id}")
    @IsUserOrAdmin
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping
    @IsUserOrAdmin
    public ResponseEntity<Page<ItemDto>> getAllItems(Pageable pageable) {
        return ResponseEntity.ok(itemService.getAllItems(pageable));
    }

    @PutMapping("/{id}")
    @IsAdmin
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequestDto requestDto) {
        return ResponseEntity.ok(itemService.updateItem(id, requestDto));
    }

    @DeleteMapping("/{id}")
    @IsAdmin
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
