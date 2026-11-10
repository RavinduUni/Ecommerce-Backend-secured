package com.ecom.dto;

import com.ecom.enums.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
public class Order {
    private Long id;
    private User user;
    private BigDecimal totalAmount;
    private OrderStatus status = OrderStatus.PENDING;
    private List<OrderItem> items = new ArrayList<>();
}
