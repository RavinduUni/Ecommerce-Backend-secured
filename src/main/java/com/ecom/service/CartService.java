package com.ecom.service;

import com.ecom.dto.CartItem;
import com.ecom.entity.CartItemEntity;
import com.ecom.entity.ProductEntity;
import com.ecom.entity.UserEntity;
import com.ecom.exception.ApiException;
import com.ecom.mapper.CartItemMapper;
import com.ecom.repository.CartItemRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemMapper cartItemMapper, CartItemRepository cartItemRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.cartItemMapper = cartItemMapper;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** Adds `quantity` units. The success message is unchanged so existing clients keep working. */
    @Transactional
    public String addToCart(Long userId, CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null || cartItem.getProduct().getId() == null) {
            throw ApiException.badRequest("Choose a product to add.");
        }
        Integer quantity = cartItem.getQuantity();
        if (quantity == null || quantity < 1) {
            throw ApiException.badRequest("Quantity must be at least 1.");
        }

        ProductEntity product = productRepository.findById(cartItem.getProduct().getId())
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .orElseThrow(() -> ApiException.notFound("That product is no longer available."));

        UserEntity user = findUser(userId);
        CartItemEntity existing = cartItemRepository.findByUserAndProduct(user, product);
        int alreadyInCart = existing != null ? existing.getQuantity() : 0;
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        // Check the new total, not just the amount being added.
        if (alreadyInCart + quantity > stock) {
            throw ApiException.conflict(stock <= 0
                    ? product.getName() + " is sold out."
                    : "Only " + stock + " of " + product.getName() + " in stock.");
        }

        CartItemEntity line = existing != null ? existing : new CartItemEntity();
        if (existing == null) {
            line.setUser(user);
            line.setProduct(product);
        }
        line.setQuantity(alreadyInCart + quantity);
        line.setPrice(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        cartItemRepository.save(line);

        return "Item added successfully";
    }

    @Transactional
    public String removeFromCart(Long userId, Long productId) {
        UserEntity user = findUser(userId);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found."));

        CartItemEntity existing = cartItemRepository.findByUserAndProduct(user, product);
        if (existing == null) {
            throw ApiException.notFound("That item isn't in your cart.");
        }
        cartItemRepository.delete(existing);
        return "Item removed successfully";
    }

    public List<CartItem> getCartItems() {
        return cartItemRepository.findAll().stream().map(cartItemMapper::toCartItemDto).toList();
    }

    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUserId(userId)
                .stream()
                .map(cartItemMapper::toCartItemDto)
                .toList();
    }

    @Transactional
    public void clearCart(Long userId) {
        userRepository.findById(userId).ifPresent(cartItemRepository::deleteByUser);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Your account could not be found."));
    }
}
