package org.authzorium.client.service;

import org.authzorium.client.dto.Pet;

import java.util.List;

public interface PetService {
    Pet save(Pet pet);
    Pet findById(Long id);
    List<Pet> findByOwnerUsername(String username);
    void deleteById(Long id);
}

