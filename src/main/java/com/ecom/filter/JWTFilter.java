package com.ecom.filter;

import com.ecom.dto.UserPrincipal;
import com.ecom.enums.UserRole;
import com.ecom.security.JsonAuthHandlers;
import com.ecom.service.JWTService;
import com.ecom.service.MyUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            // One parse checks the signature and the expiry.
            claims = jwtService.parse(authHeader.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            JsonAuthHandlers.write(response, HttpStatus.UNAUTHORIZED, "Your session has expired. Sign in again.");
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails principal = principalFromClaims(claims);

            if (principal == null) {
                // Token issued before this change (no uid/role claims): look the user up once.
                try {
                    principal = context.getBean(MyUserDetailsService.class).loadUserByUsername(claims.getSubject());
                } catch (UsernameNotFoundException e) {
                    JsonAuthHandlers.write(response, HttpStatus.UNAUTHORIZED, "Your account could not be found. Sign in again.");
                    return;
                }
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    /** Builds the user from the token's uid and role claims, or returns null if they're missing. */
    private static UserPrincipal principalFromClaims(Claims claims) {
        Object uid = claims.get(JWTService.CLAIM_USER_ID);
        Object role = claims.get(JWTService.CLAIM_ROLE);
        if (!(uid instanceof Number number) || !(role instanceof String roleName) || claims.getSubject() == null) {
            return null;
        }
        try {
            return UserPrincipal.fromToken(number.longValue(), claims.getSubject(), UserRole.valueOf(roleName));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}