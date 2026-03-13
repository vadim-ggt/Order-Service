package com.innowise.orderservice.domain.service.impl;

import com.innowise.orderservice.domain.dao.ItemRepository;
import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.domain.mapper.item.ItemMapper;
import com.innowise.orderservice.domain.service.ItemService;
import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import com.innowise.orderservice.web.dto.response.ItemDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemDto createItem(ItemRequestDto requestDto) {
        Item item = itemMapper.toEntityFromRequest(requestDto);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toDto(savedItem);
    }

    @Override
    public ItemDto getItemById(Long id) {
        Item item = itemRepository.findById(id).orElse(null);
        return itemMapper.toDto(item);
    }

    @Override
    public Page<ItemDto> getAllItems(Pageable pageable) {
        Page<Item> itemPage = itemRepository.findAll(pageable);
        return itemPage.map(itemMapper::toDto);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long id, ItemRequestDto requestDto) {
        Item item = itemRepository.findById(id).orElse(null);
        itemMapper.updateEntityFromRequest(requestDto, item);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toDto(savedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        if(!itemRepository.existsById(id)) {
            throw new EntityNotFoundException("Item not found");
        }
        itemRepository.deleteById(id);
    }
}
