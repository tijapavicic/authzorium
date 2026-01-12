package org.authzorium.service;

import org.junit.jupiter.api.Test;
import org.authzorium.service.impl.HelloServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloServiceTest {

    @Test
    public void helloReturnsExpectedMessage() {
        HelloService svc = new HelloServiceImpl();
        assertEquals("Hello, secured world!", svc.hello());
    }
}

