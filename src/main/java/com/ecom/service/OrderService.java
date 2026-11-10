package com.ecom.service;

import com.ecom.dto.Order;
import com.ecom.entity.CartItemEntity;
import com.ecom.entity.OrderEntity;
import com.ecom.entity.OrderItemEntity;
import com.ecom.entity.ProductEntity;
import com.ecom.entity.UserEntity;
import com.ecom.enums.OrderStatus;
import com.ecom.exception.ApiException;
import com.ecom.mapper.OrderMapper;
import com.ecom.repository.CartItemRepository;
import com.ecom.repository.OrderRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(CartService cartService, CartItemRepository cartItemRepository, ProductRepository productRepository,
                        UserRepository userRepository, OrderRepository orderRepository, OrderMapper orderMapper) {
        this.cartService = cartService;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Creates an order from the cart in one transaction: every line's stock is
     * reserved with an atomic UPDATE, so two buyers can't take the last unit.
     * If any line fails, everything rolls back and the cart is left as it was.
     */
    @Transactional
    public Order createOrder(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Your account could not be found."));

        List<CartItemEntity> cart = cartItemRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty.");
        }

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);

        List<OrderItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemEntity line : cart) {
            ProductEntity product = line.getProduct();
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw ApiException.conflict(product.getName() + " is no longer available. Remove it from your cart to continue.");
            }

            int reserved = productRepository.decrementStock(product.getId(), line.getQuantity());
            if (reserved == 0) {
                throw ApiException.conflict("Not enough stock for " + product.getName() + ". Update your cart and try again.");
            }

            // Charge the current price, not the price saved when the item was added.
            BigDecimal linePrice = product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            total = total.add(linePrice);
            items.add(new OrderItemEntity(null, product, line.getQuantity(), linePrice, order));
        }

        order.setItems(items);
        order.setTotalAmount(total);

        OrderEntity saved = orderRepository.save(order);
        cartService.clearCart(userId);

        return orderMapper.toOrderDto(saved);
    }
}
