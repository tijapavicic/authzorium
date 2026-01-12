package org.authzorium.controller;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Convert Spring's ProblemDetail responses into the project's ErrorResponse JSON shape.
 * This allows ProblemDetail to remain enabled while keeping a consistent error contract.
 */
@ControllerAdvice
public class ProblemDetailAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // We can inspect the declared return type, but to be safe handle any response and convert if body is ProblemDetail
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, org.springframework.http.MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ProblemDetail pd) {
            // Determine status
            int statusValue = pd.getStatus();
            HttpStatus resolved = HttpStatus.resolve(statusValue);

            // Fallback for title: prefer ProblemDetail.title, otherwise use HTTP reason phrase or a generic label
            String error = pd.getTitle();
            if (error == null || error.isBlank()) {
                error = (resolved != null) ? resolved.getReasonPhrase() : "Error";
            }

            // Fallback for detail/message: prefer ProblemDetail.detail, otherwise use the title or a generic message
            String message = pd.getDetail();
            if (message == null || message.isBlank()) {
                message = pd.getTitle();
                if (message == null || message.isBlank()) {
                    message = "Unexpected error";
                }
            }

            String path = (request != null && request.getURI() != null) ? request.getURI().getPath() : null;

            ErrorResponse er = new ErrorResponse(Instant.now(), statusValue, error, message, path);
            // Ensure response status matches ProblemDetail status
            response.setStatusCode(HttpStatusCode.valueOf(pd.getStatus()));

            return er;
        }
        return body;
    }
}
