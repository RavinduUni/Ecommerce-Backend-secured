package com.ecom.controller;

import com.ecom.dto.CartItem;
import com.ecom.dto.UserPrincipal;
import com.ecom.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The cart always belongs to the signed-in user from the JWT.
 * The old X-User-ID header is no longer read.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/")
    public ResponseEntity<String> addToCart(@AuthenticationPrincipal UserPrincipal principal, @RequestBody CartItem cartItem) {
        return ResponseEntity.ok(cartService.addToCart(principal.getId(), cartItem));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> removeFromCart(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeFromCart(principal.getId(), productId));
    }

    /** Admin only (see SecurityConfig): every user's cart lines. */
    @GetMapping("/")
    public ResponseEntity<List<CartItem>> getCartItems() {
        return ResponseEntity.ok(cartService.getCartItems());
    }

    @GetMapping("/items")
    public ResponseEntity<List<CartItem>> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getId()));
    }
}
