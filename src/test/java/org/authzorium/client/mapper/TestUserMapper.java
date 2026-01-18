package org.authzorium.client.mapper;

import org.authzorium.client.dto.User;
import org.authzorium.client.entity.UserEntity;

public class TestUserMapper implements UserMapper {

    @Override
    public UserEntity toEntity(User dto) {
        if (dto == null) return null;
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(dto.getUsername());
        userEntity.setDisplayName(dto.getDisplayName());
        userEntity.setId(dto.getId());
        return userEntity;
    }

    @Override
    public User toDto(UserEntity entity) {
        if (entity == null) return null;
        User user = new User();
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setDisplayName(entity.getDisplayName());
        return user;
    }
}

