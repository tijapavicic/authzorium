package org.authzorium.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler implements GlobalExceptionHandlerI {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @Override
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.error("Unhandled exception for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        logger.debug(FLOW, "Unhandled exception response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @Override
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.warn("Bad request for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "Bad request response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    // Handle Spring Security authentication exceptions -> 401
    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.warn("Authentication failure for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "Unauthorized response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    // Handle Spring Security authentication exceptions -> 401
    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleInsufficientUnauthorized(InsufficientAuthenticationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.warn("Insufficient authentication for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "Insufficient auth response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    // Handle JwtException specifically (invalid/expired token) -> 401 with controlled message
    @ExceptionHandler(JwtException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "Invalid or expired JWT token";
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        logger.warn("JWT validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "JWT error response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    // Handle access denied -> 403
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.warn("Access denied for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "Access denied response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    // Handle not found -> 404
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        logger.info("Entity not found for {}: {}", request.getRequestURI(), ex.getMessage());
        logger.debug(FLOW, "Entity not found response -> {}", body);
        return new ResponseEntity<>(body, status);
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                               HttpHeaders headers,
                                                               HttpStatus status,
                                                               WebRequest webRequest) {
        // Ensure we always have a sensible status and reason phrase
        HttpStatus resolvedStatus = status != null ? status : HttpStatus.BAD_REQUEST;

        String path = webRequest.getDescription(false);
        if (path != null && path.startsWith("uri=")) {
            path = path.substring(4);
        }

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());

        ErrorResponse body = new ErrorResponse(Instant.now(), resolvedStatus.value(), resolvedStatus.getReasonPhrase(), message, path);
        logger.warn("Validation failed for {}: {}", path, message);
        logger.debug(FLOW, "Validation error response -> {}", body);
        return new ResponseEntity<>(body, headers, resolvedStatus);
    }
}
