package org.authzorium.client.mapper;

import org.authzorium.client.dto.User;
import org.authzorium.client.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsDtoToEntityAndBack() {
        User dto = new User(null, "alice", "Alice A");

        UserEntity entity = mapper.toEntity(dto);
        // entity should have username/displayName set
        assertThat(entity).isNotNull();
        assertThat(entity.getUsername()).isEqualTo(dto.getUsername());
        assertThat(entity.getDisplayName()).isEqualTo(dto.getDisplayName());

        // round-trip
        User back = mapper.toDto(entity);
        assertThat(back).isNotNull();
        assertThat(back.getUsername()).isEqualTo(dto.getUsername());
        assertThat(back.getDisplayName()).isEqualTo(dto.getDisplayName());
    }
}

