package org.authzorium.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.service.PetService;
import org.authzorium.client.util.LoggingConstants;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Pet> listPets(@RequestParam(name = "ownerUsername", required = false) String ownerUsername) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling GET /pets request [{}] ownerUsername={}", requestId, ownerUsername);
        if (ownerUsername == null || ownerUsername.isBlank()) {
            // No direct service method for listing all; reuse findByOwnerUsername with null to get all via service
            return petService.findByOwnerUsername(null);
        }
        return petService.findByOwnerUsername(ownerUsername);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Pet> getPet(@PathVariable("id") Long id) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling GET /pets/{} request [{}]", id, requestId);
        Pet p = petService.findById(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Pet> createPet(@Valid @RequestBody Pet pet) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling POST /pets request [{}] pet={}", requestId, pet == null ? null : pet.getPetName());
        Pet saved = petService.save(pet);
        if (saved == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Pet> updatePet(@PathVariable("id") Long id, @Valid @RequestBody Pet pet) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling PUT /pets/{} request [{}]", id, requestId);
        // Ensure the DTO id matches path id
        pet.setId(id);
        Pet saved = petService.save(pet);
        if (saved == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable("id") Long id) {
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        log.info(LoggingConstants.flow, "Handling DELETE /pets/{} request [{}]", id, requestId);
        petService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
