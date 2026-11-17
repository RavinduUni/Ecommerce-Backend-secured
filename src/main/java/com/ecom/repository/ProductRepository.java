package com.ecom.repository;

import com.ecom.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByActiveTrue();

    Optional<ProductEntity> findByIdAndActiveTrue(Long id);

    @Query("SELECT p FROM products p WHERE p.active = true AND p.stockQuantity > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ProductEntity> searchProduct(@Param("keyword") String keyword);

    /** Each category with its number of active products, biggest first. Rows are [name, count]. */
    @Query("SELECT p.category, COUNT(p) FROM products p WHERE p.active = true AND p.category IS NOT NULL AND p.category <> '' "
            + "GROUP BY p.category ORDER BY COUNT(p) DESC, p.category ASC")
    List<Object[]> countActiveByCategory();

    /**
     * Reduces stock only if enough is left. Returns 1 when it worked, 0 when there
     * wasn't enough. Being a single UPDATE, it's safe when two orders race.
     */
    @Modifying
    @Query("UPDATE products p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}