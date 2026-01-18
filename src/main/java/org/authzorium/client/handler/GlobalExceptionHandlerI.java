package org.authzorium.client.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.authzorium.client.dto.ErrorResponse;
import org.springframework.context.MessageSourceAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

public interface GlobalExceptionHandlerI extends MessageSourceAware {
    @ExceptionHandler(Exception.class)
    @ResponseBody
    ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request);

    ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                        HttpHeaders headers,
                                                        HttpStatus status,
                                                        WebRequest webRequest);
}
