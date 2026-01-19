package org.authzorium.client.mapper;

import org.authzorium.client.dto.Pet;
import org.authzorium.client.view.PetView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PetViewMapper {
    Pet toDto(PetView view);
}
