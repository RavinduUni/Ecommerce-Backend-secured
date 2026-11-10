package com.ecom.controller;

import com.ecom.dto.Order;
import com.ecom.dto.UserPrincipal;
import com.ecom.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Turns the signed-in user's cart into an order. */
    @PostMapping("/")
    public ResponseEntity<Order> createOrder(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(orderService.createOrder(principal.getId()), HttpStatus.CREATED);
    }
}
