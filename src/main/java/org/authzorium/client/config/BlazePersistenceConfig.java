package org.authzorium.client.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import com.blazebit.persistence.view.impl.EntityViewConfigurationImpl;
import jakarta.persistence.EntityManagerFactory;
import org.authzorium.client.view.PetView;
import org.authzorium.client.view.UserView;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlazePersistenceConfig {

    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory emf) {
        // Unwrap the Hibernate SessionFactory (integration module expects a Hibernate SessionFactory)
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        return Criteria.getDefault().createCriteriaBuilderFactory(sessionFactory);
    }

    @Bean
    public EntityViewManager entityViewManager(CriteriaBuilderFactory cbf) {
        EntityViewConfiguration cfg = new EntityViewConfigurationImpl();
        // Register entity view classes
        cfg.addEntityView(PetView.class);
        cfg.addEntityView(UserView.class);
        return cfg.createEntityViewManager(cbf);
    }
}
