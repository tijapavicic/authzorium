package org.authzorium.client.mapper;

import org.authzorium.client.dto.User;
import org.authzorium.client.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(User dto);

    User toDto(UserEntity entity);
}

