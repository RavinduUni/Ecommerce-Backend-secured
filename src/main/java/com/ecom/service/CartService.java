package com.ecom.service;

import com.ecom.dto.CartItem;
import com.ecom.entity.CartItemEntity;
import com.ecom.entity.ProductEntity;
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

        ProductEntity product = productRepository.findByIdAndActiveTrue(cartItem.getProduct().getId())
                .orElseThrow(() -> ApiException.notFound("That product is no longer available."));

        CartItemEntity existing = cartItemRepository.findByUserIdAndProductId(userId, product.getId()).orElse(null);
        int alreadyInCart = existing != null ? existing.getQuantity() : 0;
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        // Check the new total, not just the amount being added.
        if (alreadyInCart + quantity > stock) {
            throw ApiException.conflict(stock <= 0
                    ? product.getName() + " is sold out."
                    : "Only " + stock + " of " + product.getName() + " in stock.");
        }

        CartItemEntity line = existing;
        if (line == null) {
            line = new CartItemEntity();
            // A reference is enough to set the foreign key; the user row isn't loaded.
            line.setUser(userRepository.getReferenceById(userId));
            line.setProduct(product);
        }
        line.setQuantity(alreadyInCart + quantity);
        line.setPrice(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        cartItemRepository.save(line);

        return "Item added successfully";
    }

    @Transactional
    public String removeFromCart(Long userId, Long productId) {
        if (cartItemRepository.deleteLine(userId, productId) == 0) {
            throw ApiException.notFound("That item isn't in your cart.");
        }
        return "Item removed successfully";
    }

    /** Admin view: every cart line, loaded with product and user in one query. */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems() {
        return cartItemRepository.findAllWithProductAndUser().stream().map(cartItemMapper::toCartItemDto).toList();
    }

    /** The user's cart, loaded with products in one query. The user is mapped inside the transaction. */
    @Transactional(readOnly = true)
    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUserIdWithProduct(userId)
                .stream()
                .map(cartItemMapper::toCartItemDto)
                .toList();
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
}