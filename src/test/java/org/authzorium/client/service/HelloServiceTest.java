package org.authzorium.client.service;

import org.authzorium.BaseTest;
import org.authzorium.client.mapper.UserMapper;
import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.impl.HelloServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloServiceTest extends BaseTest {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserMapper userMapper;

    @Test
    void helloReturnsExpectedMessage() {
        HelloService svc = new HelloServiceImpl(userRepository, userMapper);
        assertEquals("Hello, Demo UserEntity!", svc.hello());
    }
}

