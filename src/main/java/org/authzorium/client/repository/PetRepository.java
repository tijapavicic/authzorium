package org.authzorium.client.repository;

import org.authzorium.client.entity.PetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<PetEntity, Long> {
    Optional<PetEntity> findByPetName(String petName);
    List<PetEntity> findByOwnerId(Long ownerId);

    // Return a pageable result and fetch the owner relationship to avoid N+1 selects
    @EntityGraph(attributePaths = {"owner"})
    Page<PetEntity> findByOwnerId(Long ownerId, Pageable pageable);

    long countByOwnerId(Long ownerId);
}
