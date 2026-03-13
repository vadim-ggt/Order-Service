package com.innowise.orderservice.unit;

import com.innowise.orderservice.domain.dao.ItemRepository;
import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.domain.exception.ItemNotFoundException;
import com.innowise.orderservice.domain.mapper.item.ItemMapper;
import com.innowise.orderservice.domain.service.ItemService;
import com.innowise.orderservice.domain.service.impl.ItemServiceImpl;
import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import com.innowise.orderservice.web.dto.response.ItemDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item testItem;
    private ItemRequestDto requestDto;
    private ItemDto responseDto;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setPrice(new BigDecimal("100.00"));

        requestDto = new ItemRequestDto("Test Item", new BigDecimal("100.00"));
        responseDto = new ItemDto(1L, "Test Item", new BigDecimal("100.00"));
    }


    @Test
    void createItem_Success() {
        when(itemMapper.toEntityFromRequest(requestDto)).thenReturn(testItem);
        when(itemRepository.save(testItem)).thenReturn(testItem);
        when(itemMapper.toDto(testItem)).thenReturn(responseDto);

        ItemDto result = itemService.createItem(requestDto);

        assertNotNull(result);
        assertEquals(responseDto.id(), result.id());
        assertEquals(responseDto.name(), result.name());

        verify(itemMapper, times(1)).toEntityFromRequest(requestDto);
        verify(itemRepository, times(1)).save(testItem);
        verify(itemMapper, times(1)).toDto(testItem);
    }

    @Test
    void getItemById_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemMapper.toDto(testItem)).thenReturn(responseDto);

        ItemDto result = itemService.getItemById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    void getItemById_ThrowsItemNotFoundException() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        ItemNotFoundException exception = assertThrows(ItemNotFoundException.class, () -> {
            itemService.getItemById(99L);
        });

        assertEquals("Item not found with id: 99", exception.getMessage());
        verify(itemMapper, never()).toDto(any());
    }



    @Test
    void getAllItems_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> itemPage = new PageImpl<>(List.of(testItem));

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(itemMapper.toDto(testItem)).thenReturn(responseDto);

        Page<ItemDto> result = itemService.getAllItems(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(responseDto.name(), result.getContent().get(0).name());
        verify(itemRepository, times(1)).findAll(pageable);
    }

    @Test
    void updateItem_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(testItem)).thenReturn(testItem);
        when(itemMapper.toDto(testItem)).thenReturn(responseDto);

        ItemDto result = itemService.updateItem(1L, requestDto);

        assertNotNull(result);
        verify(itemRepository, times(1)).findById(1L);
        verify(itemMapper, times(1)).updateEntityFromRequest(requestDto, testItem);
        verify(itemRepository, times(1)).save(testItem);
    }

    @Test
    void updateItem_ThrowsItemNotFoundException() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> {
            itemService.updateItem(99L, requestDto);
        });

        verify(itemRepository, times(1)).findById(99L);
        verify(itemRepository, never()).save(any());
    }


    @Test
    void deleteItem_Success() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> itemService.deleteItem(1L));

        verify(itemRepository, times(1)).existsById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteItem_ThrowsItemNotFoundException() {
        when(itemRepository.existsById(99L)).thenReturn(false);

        assertThrows(ItemNotFoundException.class, () -> {
            itemService.deleteItem(99L);
        });

        verify(itemRepository, times(1)).existsById(99L);
        verify(itemRepository, never()).deleteById(any());
    }
}