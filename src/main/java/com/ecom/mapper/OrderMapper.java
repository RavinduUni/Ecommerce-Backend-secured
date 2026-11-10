package com.ecom.mapper;

import com.ecom.dto.Order;
import com.ecom.dto.OrderItem;
import com.ecom.entity.OrderEntity;
import com.ecom.entity.OrderItemEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

// uses = {} eken api kiyanawa User wa saha Product wa map karaganna kalin hadapu mappers use karanna kiyala
@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface OrderMapper {

    // ==========================================
    // 1. Entity -> DTO (Database eken Client ta)
    // ==========================================
    Order toOrderDto(OrderEntity entity);

    // Meke 'order' eka ignore karanne DTO eke eka nathi nisa saha Loop eka nawaththanna
    OrderItem toOrderItemDto(OrderItemEntity entity);

    // ==========================================
    // 2. DTO -> Entity (Client gen Database ekata)
    // ==========================================
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toOrderEntity(Order orderDto);

    @Mapping(target = "order", ignore = true) // Order eka api pahala manual set karanawa
    OrderItemEntity toOrderItemEntity(OrderItem orderItemDto);

    // ==========================================
    // 3. The Magic Trick (Boss wa set kireema)
    // ==========================================
    // Meken wenne toOrderEntity method eka wada karala iwara wunata passe meka auto run wena eka.
    @AfterMapping
    default void linkOrderItems(@MappingTarget OrderEntity order) {
        if (order.getItems() != null) {
            // Hama podi item ekatama kiyanawa "Oyage Boss thamai me Order eka" kiyala
            for (OrderItemEntity item : order.getItems()) {
                item.setOrder(order);
            }
        }
    }
}
