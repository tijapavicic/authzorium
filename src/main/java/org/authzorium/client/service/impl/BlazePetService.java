package org.authzorium.client.service.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import lombok.RequiredArgsConstructor;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.mapper.PetViewMapper;
import org.authzorium.client.mapper.OwnerViewMapper;
import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.view.PetView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnBean({EntityViewManager.class, CriteriaBuilderFactory.class})
public class BlazePetService {
    private final EntityViewManager evm;
    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;
    private final PetRepository petRepository;

    // MapStruct mappers (injected)
    private final PetViewMapper petViewMapper;
    private final OwnerViewMapper ownerViewMapper;

    public Page<Pet> findPetsByOwnerId(Long ownerId, Pageable pageable) {
        CriteriaBuilder<PetView> cb = cbf.create(em, PetView.class)
                .where("owner.id").eq(ownerId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        List<PetView> views = cb.getResultList();
        List<Pet> dtos = views.stream().map(v -> {
            if (petViewMapper != null) {
                return petViewMapper.toDto(v);
            }
            // Fallback manual mapping
            Pet p = new Pet();
            p.setId(v.getId());
            p.setPetName(v.getPetName());
            if (v.getOwner() != null) {
                if (ownerViewMapper != null) {
                    p.setOwner(ownerViewMapper.toDto(v.getOwner()));
                } else {
                    var user = new org.authzorium.client.dto.User();
                    user.setId(v.getOwner().getId());
                    user.setUsername(v.getOwner().getUsername());
                    user.setDisplayName(v.getOwner().getDisplayName());
                    p.setOwner(user);
                }
            }
            return p;
        }).collect(Collectors.toList());

        long total = petRepository.countByOwnerId(ownerId);
        return new PageImpl<>(dtos, pageable, total);
    }
}
