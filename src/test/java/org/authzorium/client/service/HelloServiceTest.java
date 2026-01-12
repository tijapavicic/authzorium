package org.authzorium.client.service;

import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.HelloService;
import org.authzorium.client.service.impl.HelloServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloServiceTest {
    @Autowired
    UserRepository userRepository;

    @Test
    void helloReturnsExpectedMessage() {
        HelloService svc = new HelloServiceImpl(userRepository);
        assertEquals("Hello, secured world!", svc.hello());
    }
}

