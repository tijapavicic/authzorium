package org.authzorium.client.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.User;
import org.authzorium.client.entity.UserEntity;
import org.authzorium.client.mapper.UserMapper;
import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.HelloService;
import org.authzorium.client.util.LoggingConstants;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service("helloService")
public class HelloServiceImpl implements HelloService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public String hello() {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.debug(LoggingConstants.flow, "HelloService.hello called [{}]", requestId);

        if (userRepository != null) {
            Optional<UserEntity> maybe = userRepository.findByUsername("demo-user");
            if (maybe.isPresent()) {
                String ret = "Hello, " + maybe.get().getDisplayName() + "!";
                log.debug(LoggingConstants.flow, "Found user for hello [{}]: {}", requestId, maybe.get().getUsername());
                return ret;
            }
        }
        log.debug(LoggingConstants.flow, "No user found for hello [{}], returning default greeting", requestId);
        return "Hello, secured world!";
    }

    @Override
    public String findByUsername(String userName) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.debug(LoggingConstants.flow, "HelloService.hello called [{}]", requestId);

        if (userRepository != null) {
            Optional<UserEntity> maybe = userRepository.findByUsername(userName);
            if (maybe.isPresent()) {
                String ret = "Hello, " + maybe.get().getDisplayName() + "!";
                log.debug(LoggingConstants.flow, "Found user for hello [{}]: {}", requestId, maybe.get().getUsername());
                return ret;
            }
        }
        log.debug(LoggingConstants.flow, "No user found for hello [{}], returning default greeting", requestId);
        return "Hello, secured world!";
    }

    @Override
    public User saveUser(User user) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.debug(LoggingConstants.flow, "HelloService.saveUser called [{}] user={}", requestId, user == null ? null : user.getUsername());
        if (user == null) return null;

        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = userRepository.save(entity);
        User out = userMapper.toDto(saved);
        log.debug(LoggingConstants.flow, "Saved user [{}] -> id={}", user.getUsername(), out.getId());
        return out;
    }
}
