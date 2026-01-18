package org.authzorium.client.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.entity.UserEntity;
import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.HelloService;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service("helloService")
public class HelloServiceImpl implements HelloService {

    private final UserRepository userRepository;
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    @Override
    public String hello() {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "HelloService.hello called [{}]", requestId);

        if (userRepository != null) {
            Optional<UserEntity> maybe = userRepository.findByUsername("demo-user");
            if (maybe.isPresent()) {
                String ret = "Hello, " + maybe.get().getDisplayName() + "!";
                log.debug(FLOW, "Found user for hello [{}]: {}", requestId, maybe.get().getUsername());
                return ret;
            }
        }
        log.debug(FLOW, "No user found for hello [{}], returning default greeting", requestId);
        return "Hello, secured world!";
    }

    @Override
    public String findByUsername(String userName) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "HelloService.hello called [{}]", requestId);

        if (userRepository != null) {
            Optional<UserEntity> maybe = userRepository.findByUsername(userName);
            if (maybe.isPresent()) {
                String ret = "Hello, " + maybe.get().getDisplayName() + "!";
                log.debug(FLOW, "Found user for hello [{}]: {}", requestId, maybe.get().getUsername());
                return ret;
            }
        }
        log.debug(FLOW, "No user found for hello [{}], returning default greeting", requestId);
        return "Hello, secured world!";
    }

    @Override
    public UserEntity saveUser(UserEntity userEntity) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "HelloService.saveUser called [{}] userEntity={}", requestId, userEntity == null ? null : userEntity.getUsername());
        if (userEntity == null) return null;
        UserEntity saved = userRepository.save(userEntity);
        log.debug(FLOW, "Saved userEntity [{}] -> id={}", userEntity.getUsername(), saved.getId());
        return saved;
    }
}
