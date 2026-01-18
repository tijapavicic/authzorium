package org.authzorium.client.service;

import org.authzorium.client.dto.User;

public interface HelloService {
    String hello();

    String findByUsername(String userName);

    // Persist a user DTO and return the saved DTO (with id populated)
    User saveUser(User user);
}
