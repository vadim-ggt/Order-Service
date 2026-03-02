package com.innowise.orderservice.domain.service;

import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import com.innowise.orderservice.web.dto.response.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {

    ItemDto createItem(ItemRequestDto requestDto);

    ItemDto getItemById(Long id);

    Page<ItemDto> getAllItems(Pageable pageable);

    ItemDto updateItem(Long id, ItemRequestDto requestDto);

    void deleteItem(Long id);
}
