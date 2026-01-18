package org.authzorium.client.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.mapper.PetMapper;
import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.service.PetService;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetMapper petMapper;
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    @Override
    public Pet save(Pet pet) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "PetService.save called [{}] pet={}", requestId, pet == null ? null : pet.getPetName());
        if (pet == null) return null;
        PetEntity entity = petMapper.toEntity(pet);
        PetEntity saved = petRepository.save(entity);
        return petMapper.toDto(saved);
    }

    @Override
    public Pet findById(Long id) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "PetService.findById called [{}] id={}", requestId, id);
        Optional<PetEntity> maybe = petRepository.findById(id);
        return maybe.map(petMapper::toDto).orElse(null);
    }

    @Override
    public List<Pet> findByOwnerUsername(String username) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "PetService.findByOwnerUsername called [{}] username={}", requestId, username);
        // If username not provided, return all pets
        List<PetEntity> all = petRepository.findAll();
        if (username == null || username.isBlank()) {
            return all.stream().map(petMapper::toDto).toList();
        }
        return all.stream()
                .filter(pe -> pe.getOwner() != null && username.equals(pe.getOwner().getUsername()))
                .map(petMapper::toDto)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        String requestId = MDC.get("requestId");
        log.debug(FLOW, "PetService.deleteById called [{}] id={}", requestId, id);
        petRepository.deleteById(id);
    }
}
