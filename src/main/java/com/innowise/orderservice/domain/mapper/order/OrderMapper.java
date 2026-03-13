package com.innowise.orderservice.domain.mapper.order;

import com.innowise.orderservice.domain.entity.Order;
import com.innowise.orderservice.domain.mapper.GenericMapper;
import com.innowise.orderservice.web.dto.request.OrderRequestDto;
import com.innowise.orderservice.web.dto.response.OrderResponseDto;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GenericMapper.class, uses = {OrderItemMapper.class})
public interface OrderMapper extends GenericMapper<Order, OrderResponseDto> {

    @Mapping(target = "user", source = "userInfo")
    OrderResponseDto toDtoWithUser(Order order, UserInfoDto userInfo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    Order toEntityFromRequest(OrderRequestDto requestDto);
}
