package org.authzorium.client.service.impl;

import lombok.RequiredArgsConstructor;
import org.authzorium.client.model.User;
import org.authzorium.client.repository.UserRepository;
import org.authzorium.client.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@RequiredArgsConstructor
@Service("helloService")
public class HelloServiceImpl implements HelloService {

    @Autowired
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    @Override
    public String hello() {
        String requestId = MDC.get("requestId");
        logger.debug(FLOW, "HelloService.hello called [{}]", requestId);

        if (userRepository != null) {
            Optional<User> maybe = userRepository.findByUsername("demo-user");
            if (maybe.isPresent()) {
                String ret = "Hello, " + maybe.get().getDisplayName() + "!";
                logger.debug(FLOW, "Found user for hello [{}]: {}", requestId, maybe.get().getUsername());
                return ret;
            }
        }
        logger.debug(FLOW, "No user found for hello [{}], returning default greeting", requestId);
        return "Hello, secured world!";
    }
}
