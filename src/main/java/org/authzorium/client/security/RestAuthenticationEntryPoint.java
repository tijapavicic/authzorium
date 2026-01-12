package org.authzorium.client.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.authzorium.client.controller.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    public RestAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        // Support java.time types like Instant
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String requestId = request.getHeader("X-Request-ID");
        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null || remoteIp.isBlank()) remoteIp = request.getRemoteAddr();

        // Log the unauthorized access without exception object to avoid stacktrace in standard logs
        logger.warn("Unauthorized request to {} from {} headers={} : {}", request.getRequestURI(), remoteIp, request.getHeaderNames(), authException.getMessage());
        // If debug enabled, log short form of the exception (no stacktrace)
        logger.debug("Unauthorized request for {} -> {}", request.getRequestURI(), authException.toString());

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        // Normalize message for certain known auth exceptions to keep contract deterministic
        String message;
        if (authException instanceof InsufficientAuthenticationException) {
            message = "Authentication credentials are missing or insufficient";
        } else {
            message = authException.getMessage();
        }

        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);

        // Log the body at debug with FLOW marker so request flow file captures it
        logger.debug(FLOW, "Unauthorized response [{}] -> {}", requestId, objectMapper.writeValueAsString(body));
    }
}
