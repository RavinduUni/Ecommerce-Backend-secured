package com.ecom.mapper;

import com.ecom.dto.CartItem;
import com.ecom.entity.CartItemEntity;
import org.mapstruct.Mapper;

/**
 * Entity to DTO only. Cart lines are created in CartService, never mapped from a request.
 * Uses UserMapper.toUserSummary for the user and ProductMapper for the product.
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface CartItemMapper {
    CartItem toCartItemDto(CartItemEntity entity);
}