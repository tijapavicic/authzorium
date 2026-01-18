package org.authzorium.client.view;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.CriteriaBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PetViewIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EntityViewManager evm;

    @Autowired
    private CriteriaBuilderFactory cbf;

    @Test
    @Transactional
    void entityViewLoadsPetWithOwner() {
        UserEntity user = new UserEntity();
        user.setUsername("jdoe");
        user.setDisplayName("John Doe");
        em.persist(user);

        PetEntity pet = new PetEntity();
        pet.setPetName("Fido");
        pet.setOwner(user);
        em.persist(pet);
        em.flush();
        em.clear();

        PetView view = evm.find(em, PetView.class, pet.getId());
        assertThat(view).isNotNull();
        assertThat(view.getPetName()).isEqualTo("Fido");
        assertThat(view.getOwner()).isNotNull();
        assertThat(view.getOwner().getUsername()).isEqualTo("jdoe");
    }
}
