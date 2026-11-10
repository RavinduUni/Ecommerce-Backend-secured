package com.ecom.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class OrderItem {
    private Long id;
    private Product product;
    private Integer quantity;
    private BigDecimal price;
}
