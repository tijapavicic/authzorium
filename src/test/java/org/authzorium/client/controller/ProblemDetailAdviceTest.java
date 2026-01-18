package org.authzorium.client.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ProblemDetailAdviceTest {

    @Test
    void convertsProblemDetail_withMissingFields_toErrorResponse() throws Exception {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        // leave title/detail null to exercise fallbacks

        ProblemDetailAdvice advice = new ProblemDetailAdvice();

        // We need dummy MethodParameter and mock request/response objects
        MethodParameter mp = new MethodParameter(Dummy.class.getMethod("m"), -1);
        MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", "/test/path");
        MockHttpServletResponse servletResp = new MockHttpServletResponse();
        ServerHttpRequest req = new ServletServerHttpRequest(servletReq);
        ServerHttpResponse resp = new ServletServerHttpResponse(servletResp);

        Object out = advice.beforeBodyWrite(pd, mp, null, StringHttpMessageConverter.class, req, resp);
        assertNotNull(out);
        assertInstanceOf(ErrorResponse.class, out);

        ErrorResponse er = (ErrorResponse) out;
        assertEquals(400, er.getStatus());
        // title missing -> fallback to reason phrase
        assertEquals("Bad Request", er.getError());
        // detail missing -> fallback to title or generic
        assertNotNull(er.getMessage());
        assertEquals("/test/path", er.getPath());
        assertNotNull(er.getTimestamp());
    }

    static class Dummy { public void m() {} }
}
