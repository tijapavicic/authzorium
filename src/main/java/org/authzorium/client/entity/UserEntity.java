package org.authzorium.client.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.*;

@Setter
@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "app_user")
public class UserEntity {

    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String username;
    @NonNull
    private String displayName;

}
