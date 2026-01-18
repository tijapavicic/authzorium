package org.authzorium.client.service;

import org.authzorium.BaseTest;
import org.authzorium.client.mapper.UserMapper;
import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.impl.HelloServiceImpl;
import org.authzorium.client.entity.UserEntity;
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
        // Ensure repository contains the expected demo-user for this unit test
        userRepository.deleteAll();
        userRepository.save(new UserEntity("demo-user", "Demo UserEntity"));

        HelloService svc = new HelloServiceImpl(userRepository, userMapper);
        assertEquals("Hello, Demo UserEntity!", svc.hello());
    }
}
