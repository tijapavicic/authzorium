package org.authzorium.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.HelloResponse;
import org.authzorium.client.dto.User;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.service.HelloService;
import org.authzorium.client.util.LoggingConstants;

import org.slf4j.MDC;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final HelloService helloService;

    public UserController(HelloService helloService, ConfigurableApplicationContext ctx) {
        this.helloService = helloService;
    }

    // Map username as a path variable to make the endpoint explicit and RESTful.
    @GetMapping(value = "/findByUsername/{userName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HelloResponse findByUsername(@PathVariable("userName") String userName) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling /findByUsername request [{}] for userName={}", requestId, userName);

        User user = helloService.findUserByUsername(userName);
        String greeting = user != null ? "Hello, " + user.getDisplayName() + "!" : "Hello, secured world!";
        HelloResponse resp = new HelloResponse(greeting, userName);
        log.debug(LoggingConstants.flow, "/findByUsername response [{}] -> {}", requestId, resp);
        return resp;
    }

    // Create a new user - accepts JSON body and returns the saved user (with id)
    // With the class-level @RequestMapping("/users"), an empty @PostMapping maps to POST /users
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> saveUser(@Valid @RequestBody User user) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling /users POST request [{}] user={}", requestId, user == null ? null : user.getUsername());

        User saved = helloService.saveUser(user);
        if (saved == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(saved);
    }

    // Fetch all pets of a user by userName
    @GetMapping(value = "/{userName}/pets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Pet>> getUserPets(@PathVariable("userName") String userName) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling /users/{}/pets request [{}]", userName, requestId);
        List<Pet> pets = helloService.getPetsOfUser(userName);
        return ResponseEntity.ok(pets);
    }
}
