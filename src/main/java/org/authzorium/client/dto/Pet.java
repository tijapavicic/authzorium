package org.authzorium.client.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Pet {
    private Long id;

    @NotBlank
    private String petName;

    private User owner;
}
