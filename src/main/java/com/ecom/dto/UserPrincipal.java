package com.ecom.dto;

import com.ecom.entity.UserEntity;
import com.ecom.enums.UserRole;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final UserEntity user;

    public UserPrincipal(UserEntity user) {
        this.user = user;
    }

    /** ROLE_ADMIN or ROLE_CUSTOMER, so hasRole("ADMIN") works in SecurityConfig. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRole role = user.getRole() != null ? user.getRole() : UserRole.CUSTOMER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /** The signed-in user's database id (taken from the JWT, never from a request header). */
    public Long getId() {
        return user.getId();
    }

    public UserEntity getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }
}
