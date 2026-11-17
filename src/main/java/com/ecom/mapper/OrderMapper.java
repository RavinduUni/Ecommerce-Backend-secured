package com.ecom.mapper;

import com.ecom.dto.Order;
import com.ecom.dto.OrderItem;
import com.ecom.entity.OrderEntity;
import com.ecom.entity.OrderItemEntity;
import org.mapstruct.Mapper;

/**
 * Entity to DTO only. Orders are built from the cart in OrderService.
 * Uses UserMapper.toUserSummary for the user and ProductMapper for each item's product.
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface OrderMapper {
    Order toOrderDto(OrderEntity entity);

    OrderItem toOrderItemDto(OrderItemEntity entity);
}