package org.authzorium.client.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    private Long id;

    @NonNull
    private String username;
    @NonNull
    private String displayName;
}
