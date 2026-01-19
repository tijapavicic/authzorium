package org.authzorium.client.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import org.authzorium.client.entity.PetEntity;

@EntityView(PetEntity.class)
public interface PetView {
    @IdMapping
    Long getId();

    String getPetName();

    @Mapping("owner")
    OwnerView getOwner();
}
