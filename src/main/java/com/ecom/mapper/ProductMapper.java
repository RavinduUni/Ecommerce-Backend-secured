package com.ecom.mapper;

import com.ecom.dto.Product;
import com.ecom.dto.User;
import com.ecom.entity.ProductEntity;
import com.ecom.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {
    Product toProductDto(ProductEntity entity);

    @Mapping(target = "imageFileId", ignore = true)
    ProductEntity toProductEntity(Product productDto);

    @Mapping(target = "imageFileId", ignore = true)
    void updateProductEntityFromDto(Product updatedProduct, @MappingTarget ProductEntity existingProduct);
}
