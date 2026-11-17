package com.ecom.dto;

import com.ecom.entity.UserEntity;
import com.ecom.enums.UserRole;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The signed-in user. Built either from the database (at sign-in, where the
 * password is needed) or straight from the JWT claims (every other request).
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final @Nullable String password;
    private final UserRole role;

    /** From the database: used by the sign-in check. */
    public UserPrincipal(UserEntity user) {
        this(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    private UserPrincipal(Long id, String email, @Nullable String password, @Nullable UserRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : UserRole.CUSTOMER;
    }

    /** From a verified JWT: no database access, no password. */
    public static UserPrincipal fromToken(Long id, String email, UserRole role) {
        return new UserPrincipal(id, email, null, role);
    }

    /** ROLE_ADMIN or ROLE_CUSTOMER, so hasRole("ADMIN") works in SecurityConfig. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}