package org.authzorium.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerUnitTest {

    @Test
    void whenValidationFails_handlerProducesErrorResponseShape() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // Build a MethodParameter for a dummy controller method parameter annotated with @Valid
        Method method = DummyController.class.getMethod("dummy", Payload.class);
        MethodParameter methodParam = new MethodParameter(method, 0);

        // Create a binding result with a field error
        Payload payload = new Payload();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(payload, "payload");
        bindingResult.addError(new FieldError("payload", "required", "must not be null"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);

        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/test/validate");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, headers, status, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(ErrorResponse.class, response.getBody(), "body should be ErrorResponse");

        ErrorResponse er = (ErrorResponse) response.getBody();
        assertNotNull(er.getTimestamp());
        assertEquals(400, er.getStatus());
        assertEquals("Bad Request", er.getError());
        assertTrue(er.getMessage().contains("required"));
        assertEquals("/test/validate", er.getPath());
    }

    @Test
    void whenEntityNotFound_thenReturns404ErrorResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/things/123");

        EntityNotFoundException ex = new EntityNotFoundException("not found");
        ResponseEntity<ErrorResponse> resp = handler.handleEntityNotFound(ex, req);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        ErrorResponse er = resp.getBody();
        assertEquals(404, er.getStatus());
        assertEquals("Not Found", er.getError());
        assertEquals("not found", er.getMessage());
        assertEquals("/things/123", er.getPath());
        assertNotNull(er.getTimestamp());
    }

    @Test
    void whenJwtException_thenReturns401WithControlledMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/resource");

        JwtException ex = new JwtException("original detail");
        ResponseEntity<ErrorResponse> resp = handler.handleJwtException(ex, req);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        ErrorResponse er = resp.getBody();
        assertEquals(401, er.getStatus());
        assertEquals("Unauthorized", er.getError());
        assertEquals("Invalid or expired JWT token", er.getMessage());
        assertEquals("/api/resource", er.getPath());
    }

    @Test
    void whenAuthenticationException_thenReturns401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/secure");

        AuthenticationCredentialsNotFoundException ex = new AuthenticationCredentialsNotFoundException("no creds");
        ResponseEntity<ErrorResponse> resp = handler.handleUnauthorized(ex, req);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        ErrorResponse er = resp.getBody();
        assertNotNull(er);
        assertEquals(401, er.getStatus());
        assertEquals("Unauthorized", er.getError());
        assertEquals("no creds", er.getMessage());
        assertEquals("/secure", er.getPath());
    }

    @Test
    void whenAccessDenied_thenReturns403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin");

        AccessDeniedException ex = new AccessDeniedException("denied");
        ResponseEntity<ErrorResponse> resp = handler.handleAccessDenied(ex, req);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        ErrorResponse er = resp.getBody();
        assertNotNull(er);
        assertEquals(403, er.getStatus());
        assertEquals("Forbidden", er.getError());
        assertEquals("denied", er.getMessage());
        assertEquals("/admin", er.getPath());
    }

    public static class DummyController {
        // method exists only for MethodParameter construction in the test
        public void dummy(@Valid Payload payload) {
            // no-op
        }
    }

    public static class Payload {
        @NotNull
        public String required;
    }
}
