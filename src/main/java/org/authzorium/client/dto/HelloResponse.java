package org.authzorium.client.dto;

import lombok.Getter;

@Getter
public class HelloResponse {

    private final String greeting;
    private final String port;

    public HelloResponse(String greeting, String port) {
        this.greeting = greeting;
        this.port = port;
    }

}

