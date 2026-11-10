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
public interface UserMapper {

    User toUserDto(UserEntity entity);
    UserEntity toUserEntity(User userDto);
    @Mapping(target = "id", ignore = true) // User ge ID eka update karanna epa
    @Mapping(target = "address.id", ignore = true) // Address ekak thiyenawanm eke ID eka update karanna epa
    @Mapping(target = "createdAt", ignore = true) // Created date eka update karanna epa
    @Mapping(target = "updatedAt", ignore = true) // Updated date eka auto update wena nisa meken update karanna epa
    void updateUserEntityFromDto(User updatedUser, @MappingTarget UserEntity existingUser);
}
