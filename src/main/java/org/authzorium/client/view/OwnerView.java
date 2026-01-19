package org.authzorium.client.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import org.authzorium.client.entity.UserEntity;

@EntityView(UserEntity.class)
public interface OwnerView {
    @IdMapping
    Long getId();
    String getUsername();
    String getDisplayName();
}
