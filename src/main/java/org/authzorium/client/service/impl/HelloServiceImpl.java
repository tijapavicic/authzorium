package org.authzorium.service.impl;

import org.authzorium.model.User;
import org.authzorium.repository.UserRepository;
import org.authzorium.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("helloService")
public class HelloServiceImpl implements HelloService {

    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    // Default constructor for simple unit tests where repository isn't available
    public HelloServiceImpl() {
        this.userRepository = null;
    }

    // Constructor for injection
    @Autowired
    public HelloServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
