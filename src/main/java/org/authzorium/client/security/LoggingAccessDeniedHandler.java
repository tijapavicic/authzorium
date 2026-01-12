package org.authzorium.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.authzorium.controller.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

@Component
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    public LoggingAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        String requestId = request.getHeader("X-Request-ID");
        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null || remoteIp.isBlank()) remoteIp = request.getRemoteAddr();

        // Log access denied without exception object to avoid stacktrace in normal logs
        logger.warn("Access denied to {} from {} headers={} : {}", request.getRequestURI(), remoteIp, request.getHeaderNames(), accessDeniedException.getMessage());
        // Debug short form
        logger.debug("AccessDeniedException for {} -> {}", request.getRequestURI(), accessDeniedException.toString());

        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), accessDeniedException.getMessage(), request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);

        logger.debug(FLOW, "Forbidden response [{}] -> {}", requestId, objectMapper.writeValueAsString(body));
    }
}
