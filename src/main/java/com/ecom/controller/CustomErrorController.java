package com.ecom.controller;

import com.ecom.exception.ApiError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Last-resort JSON error page for errors that happen outside controllers
 * (the GlobalExceptionHandler covers errors inside them).
 * Mapped to exactly "/error" for every HTTP method.
 */
@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String message = status.is5xxServerError() ? "Something went wrong on the server." : status.getReasonPhrase();
        return ResponseEntity.status(status).body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
