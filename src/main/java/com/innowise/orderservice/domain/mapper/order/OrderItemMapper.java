package com.innowise.orderservice.domain.mapper.order;

import com.innowise.orderservice.domain.entity.OrderItem;
import com.innowise.orderservice.domain.mapper.GenericMapper;
import com.innowise.orderservice.domain.mapper.item.ItemMapper;
import com.innowise.orderservice.web.dto.response.OrderItemResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = GenericMapper.class, uses = {ItemMapper.class})
public interface OrderItemMapper extends GenericMapper<OrderItem, OrderItemResponseDto> {
}
