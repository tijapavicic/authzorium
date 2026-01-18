package org.authzorium.client.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;

@EntityView(org.authzorium.client.entity.UserEntity.class)
public interface UserView {

    @IdMapping
    Long getId();

    String getUsername();

    String getDisplayName();
}
