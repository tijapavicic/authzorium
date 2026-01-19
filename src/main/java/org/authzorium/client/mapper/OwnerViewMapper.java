package org.authzorium.client.mapper;

import org.authzorium.client.dto.User;
import org.authzorium.client.view.OwnerView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OwnerViewMapper {
    User toDto(OwnerView view);
}
