package com.ecom.repository;

import com.ecom.entity.CartItemEntity;
import com.ecom.entity.ProductEntity;
import com.ecom.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    CartItemEntity findByUserAndProduct(UserEntity userEntity, ProductEntity productEntity);

    List<CartItemEntity> findByUserId(Long userId);

    void deleteByUser(UserEntity userEntity);
}
