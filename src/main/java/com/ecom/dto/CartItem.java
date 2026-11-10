package com.ecom.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItem {
    private Long id;
    private User user;
    private Product product;
    private Integer quantity;
    private BigDecimal price;
}
