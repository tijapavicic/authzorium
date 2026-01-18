package org.authzorium.client.service;

import org.authzorium.client.entity.UserEntity;

public interface HelloService {
    String hello();

    String findByUsername(String userName);

    // Persist a userEntity and return the saved entity (with id populated)
    UserEntity saveUser(UserEntity userEntity);
}
