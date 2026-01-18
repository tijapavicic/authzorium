package org.authzorium.client.mapper;

import org.authzorium.client.dto.Pet;
import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PetMapper {

    @Mapping(source = "owner", target = "owner")
    PetEntity toEntity(Pet dto);

    @Mapping(source = "owner", target = "owner")
    Pet toDto(PetEntity entity);

    // Keep helper in case of nulls; MapStruct will use UserMapper for nested mapping
}
