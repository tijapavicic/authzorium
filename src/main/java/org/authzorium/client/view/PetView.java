package org.authzorium.client.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;

@EntityView(org.authzorium.client.entity.PetEntity.class)
public interface PetView {

    @IdMapping
    Long getId();

    String getPetName();

    UserView getOwner();
}
