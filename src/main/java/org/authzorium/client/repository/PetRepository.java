package org.authzorium.client.repository;

import org.authzorium.client.entity.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<PetEntity, Long> {
    Optional<PetEntity> findByPetName(String petName);
    List<PetEntity> findByOwnerId(Long ownerId);
}

