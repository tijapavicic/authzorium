package org.authzorium.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.authzorium.client.dto.HelloResponse;
import org.authzorium.client.service.HelloService;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/hello")
public class HelloController {

    private final HelloService helloService;
    private final ConfigurableEnvironment env;
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    public HelloController(HelloService helloService, ConfigurableApplicationContext ctx) {
        this.helloService = helloService;
        this.env = ctx.getEnvironment();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public HelloResponse hello() {
        String requestId = MDC.get("requestId");
        log.info(FLOW, "Handling /hello request [{}]", requestId);

        String greeting = helloService.hello();

        String port = env.getProperty("local.server.port");
        if (port == null) {
            port = env.getProperty("server.port", "8081");
        }

        HelloResponse resp = new HelloResponse(greeting, port);
        log.debug(FLOW, "/hello response [{}] -> {}", requestId, resp);
        return resp;
    }
}