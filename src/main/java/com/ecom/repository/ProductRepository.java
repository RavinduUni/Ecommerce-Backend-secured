package com.ecom.repository;

import com.ecom.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByActiveTrue();

    @Query("SELECT p FROM products p WHERE p.active = true AND p.stockQuantity > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ProductEntity> searchProduct(@Param("keyword") String keyword);

    /**
     * Reduces stock only if enough is left. Returns 1 when it worked, 0 when there
     * wasn't enough. Being a single UPDATE, it's safe when two orders race.
     */
    @Modifying
    @Query("UPDATE products p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
