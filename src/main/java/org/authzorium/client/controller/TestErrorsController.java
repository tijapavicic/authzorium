package org.authzorium.client.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestErrorsController {

    private final Logger logger = LoggerFactory.getLogger(TestErrorsController.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    public static class TestDto {
        @jakarta.validation.constraints.NotBlank
        private String name;

        public TestDto() {}

        public TestDto(String name) { this.name = name; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validate(@Valid @RequestBody TestDto dto) {
        String requestId = MDC.get("requestId");
        logger.info(FLOW, "Validation endpoint called [{}] with payload: {}", requestId, dto);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/notfound")
    public ResponseEntity<String> notFound() {
        String requestId = MDC.get("requestId");
        logger.info(FLOW, "NotFound endpoint called [{}] - will throw EntityNotFoundException", requestId);
        throw new EntityNotFoundException("resource not found");
    }

    @GetMapping("/problem")
    public ResponseEntity<ProblemDetail> problem() {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.I_AM_A_TEAPOT);
        pd.setTitle("I'm a teapot");
        pd.setDetail("Short and stout");
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body(pd);
    }

    @GetMapping("/problem-empty")
    public ResponseEntity<ProblemDetail> problemEmpty() {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        // explicitly clear title/detail to exercise fallback logic in ProblemDetailAdvice
        pd.setTitle(null);
        pd.setDetail(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @GetMapping("/throw-jwt")
    public ResponseEntity<String> throwJwt() {
        throw new JwtException("simulated jwt failure");
    }

    @GetMapping("/insufficient-auth")
    public ResponseEntity<String> insufficientAuth() {
        throw new InsufficientAuthenticationException("no credentials provided");
    }
}
