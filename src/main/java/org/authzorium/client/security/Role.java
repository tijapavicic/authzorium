package org.authzorium.client.security;

import lombok.Getter;

/**
 * Centralized enum of application roles.
 */

@Getter
public enum Role {
    ADMIN("admin", "Administrator with full access"),
    USER("user", "Regular user with limited access");

    private final String flag;
    private final String description;

    Role(String flag, String description) {
        this.flag = flag;
        this.description = description;
    }


    public String authority() {
        return "ROLE_" + this.name();
    }

    @Override
    public String toString() {
        return this.name();
    }
}
