package com.ecom.mapper;

import com.ecom.dto.Product;
import com.ecom.dto.User;
import com.ecom.entity.ProductEntity;
import com.ecom.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {
    Product toProductDto(ProductEntity entity);
    ProductEntity toProductEntity(Product productDto);
    void updateProductEntityFromDto(Product updatedProduct, @MappingTarget ProductEntity existingProduct);
}
