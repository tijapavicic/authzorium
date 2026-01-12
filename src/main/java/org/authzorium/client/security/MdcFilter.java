package org.authzorium.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Adds request-scoped MDC entries so logs include requestId and remoteIp.
 * - Uses X-Request-ID header if provided, otherwise generates a UUID.
 * - Extracts remote IP from X-Forwarded-For when present.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_REMOTE_IP = "remoteIp";

    private final Logger logger = LoggerFactory.getLogger(MdcFilter.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null || remoteIp.isBlank()) {
            remoteIp = request.getRemoteAddr();
        } else {
            // if multiple addresses, take the first
            int idx = remoteIp.indexOf(',');
            if (idx != -1) {
                remoteIp = remoteIp.substring(0, idx).trim();
            }
        }

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_REMOTE_IP, remoteIp);
        // echo request id back to client for correlation
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Detailed start log to trace the request flow
        logger.debug(FLOW, "Start request: [{}] {} {} from {}", requestId, request.getMethod(), request.getRequestURI(), remoteIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Log the completed status so flows can be correlated in logs
            try {
                int status = response.getStatus();
                logger.debug(FLOW, "End request: [{}] {} {} -> status={} (from {})", requestId, request.getMethod(), request.getRequestURI(), status, remoteIp);
            } catch (Exception e) {
                logger.debug(FLOW, "End request (failed to read status) [{}] {} {}", requestId, request.getMethod(), request.getRequestURI());
            }
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_REMOTE_IP);
        }
    }
}
