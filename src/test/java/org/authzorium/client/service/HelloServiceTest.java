package org.authzorium.client.service;

import org.authzorium.BaseTest;
import org.authzorium.client.mapper.PetMapper;
import org.authzorium.client.mapper.UserMapper;
import org.authzorium.client.repository.PetRepository;
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
    @Autowired
    PetRepository petRepository;
    @Autowired
    PetMapper petMapper;

    @Test
    void helloReturnsExpectedMessage() {
        // Ensure repository contains the expected demo-user for this unit test
        userRepository.deleteAll();
        userRepository.save(new UserEntity("demo-user", "Demo User Entity"));

        HelloService svc = new HelloServiceImpl(userRepository, userMapper, petRepository, petMapper);
        assertEquals("Hello, Demo UserEntity!", svc.hello());
    }
}
