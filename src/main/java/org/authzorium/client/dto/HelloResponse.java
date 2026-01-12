package org.authzorium.client.dto;

public class HelloResponse {

    private final String greeting;
    private final String port;

    public HelloResponse(String greeting, String port) {
        this.greeting = greeting;
        this.port = port;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getPort() {
        return port;
    }
}

