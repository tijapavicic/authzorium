package org.authzorium.client.service;

import org.authzorium.client.dto.User;
import org.authzorium.client.dto.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HelloService {
    String hello();

    String findByUsername(String userName);

    // Persist a user DTO and return the saved DTO (with id populated)
    User saveUser(User user);

    // Fetch all pets of a user by userName (paginated)
    Page<Pet> getPetsOfUser(String userName, Pageable pageable);

    // Find user by username and return User DTO
    User findUserByUsername(String userName);
}
