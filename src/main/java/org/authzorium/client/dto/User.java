package org.authzorium.client.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    private Long id;
    @NotBlank
    private String username;
    @NotBlank
    private String displayName;
}
