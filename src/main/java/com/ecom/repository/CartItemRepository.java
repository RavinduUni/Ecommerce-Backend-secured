package com.ecom.repository;

import com.ecom.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    /** One line for this user and product (uses the unique index from V4). */
    Optional<CartItemEntity> findByUserIdAndProductId(Long userId, Long productId);

    /** The user's cart with each product loaded in the same query (no N+1). */
    @Query("select c from CartItemEntity c join fetch c.product where c.user.id = :userId order by c.id")
    List<CartItemEntity> findByUserIdWithProduct(@Param("userId") Long userId);

    /** Every cart line with its product and user, for the admin "Open carts" view. */
    @Query("select c from CartItemEntity c join fetch c.product join fetch c.user order by c.user.id, c.id")
    List<CartItemEntity> findAllWithProductAndUser();

    /** Deletes one line directly, without loading it first. Returns rows deleted (0 or 1). */
    @Modifying
    @Query("delete from CartItemEntity c where c.user.id = :userId and c.product.id = :productId")
    int deleteLine(@Param("userId") Long userId, @Param("productId") Long productId);

    /** Empties a user's cart in one statement. */
    @Modifying
    @Query("delete from CartItemEntity c where c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}