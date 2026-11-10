package com.ecom.security;

import com.ecom.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** JSON responses for 401 (not signed in / bad token) and 403 (signed in, not allowed). */
public final class JsonAuthHandlers {

    private JsonAuthHandlers() {}

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) ->
                write(response, HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) ->
                write(response, HttpStatus.FORBIDDEN, "You don't have permission to do that.");
    }

    public static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(ApiError.of(status.value(), status.getReasonPhrase(), message).toJson());
    }
}
