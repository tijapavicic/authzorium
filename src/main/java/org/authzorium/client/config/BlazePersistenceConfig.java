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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "enable.blaze.integration", havingValue = "true")
public class BlazePersistenceConfig {

    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory emf) {
        // Unwrap the Hibernate SessionFactory (integration module expects a Hibernate SessionFactory)
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        try {
            return Criteria.getDefault().createCriteriaBuilderFactory(sessionFactory);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Provide a clearer message when the Blaze-Persistence Hibernate integrator is missing
            String msg = "Blaze-Persistence Hibernate integration not found on the classpath. "
                    + "If you intended to enable Blaze integration, activate the 'blaze-integration' profile and ensure the "
                    + "provider artifact (com.blazebit:blaze-persistence-integration-hibernate6:${blaze.persistence.version}) is resolvable. "
                    + "Original error: " + ex.getMessage();
            throw new BeanCreationException(msg, ex);
        }
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
