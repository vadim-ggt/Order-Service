package com.innowise.orderservice.domain.mapper.item;

import com.innowise.orderservice.domain.entity.Item;
import com.innowise.orderservice.domain.mapper.GenericMapper;
import com.innowise.orderservice.web.dto.request.ItemRequestDto;
import com.innowise.orderservice.web.dto.response.ItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GenericMapper.class)
public interface ItemMapper extends GenericMapper<Item, ItemDto> {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntityFromRequest(ItemRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ItemRequestDto requestDto, @MappingTarget Item item);
}
