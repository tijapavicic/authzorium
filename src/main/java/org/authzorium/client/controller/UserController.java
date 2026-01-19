package org.authzorium.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.HelloResponse;
import org.authzorium.client.dto.User;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.service.HelloService;
import org.authzorium.client.service.impl.BlazePetService;
import org.authzorium.client.util.LoggingConstants;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final HelloService helloService;
    private BlazePetService blazePetService; // may be null when Blaze isn't on the classpath

    public UserController(HelloService helloService) {
        this.helloService = helloService;
    }

    @Autowired(required = false)
    public void setBlazePetService(BlazePetService blazePetService) {
        this.blazePetService = blazePetService;
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

    // Fetch all pets of a user by userName (paginated, JPA)
    @GetMapping(value = "/{userName}/pets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Pet>> getUserPets(@PathVariable("userName") String userName, Pageable pageable) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling /users/{}/pets request [{}]", userName, requestId);
        Page<Pet> pets = helloService.getPetsOfUser(userName, pageable);
        return ResponseEntity.ok(pets);
    }

    // Fetch all pets of a user by userName using Blaze (if available). Falls back to JPA pageable endpoint.
    @GetMapping(value = "/{userName}/pets-blaze", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Pet>> getUserPetsBlaze(@PathVariable("userName") String userName, Pageable pageable) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling /users/{}/pets-blaze request [{}]", userName, requestId);

        // Resolve user id first
        var user = helloService.findUserByUsername(userName);
        if (user == null) return ResponseEntity.notFound().build();
        Long userId = user.getId();

        if (blazePetService != null) {
            Page<Pet> pets = blazePetService.findPetsByOwnerId(userId, pageable);
            return ResponseEntity.ok(pets);
        }

        // Fallback to JPA pageable method
        Page<Pet> pets = helloService.getPetsOfUser(userName, pageable);
        return ResponseEntity.ok(pets);
    }
}
