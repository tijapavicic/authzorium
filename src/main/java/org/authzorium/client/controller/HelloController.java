package org.authzorium.client.controller;

import org.authzorium.client.dto.HelloResponse;
import org.authzorium.client.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final HelloService helloService;
    private final Environment env;
    private final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final Marker FLOW = MarkerFactory.getMarker("FLOW");

    public HelloController(HelloService helloService, Environment env) {
        this.helloService = helloService;
        this.env = env;
    }

    @GetMapping("/hello")
    public HelloResponse hello() {
        String requestId = MDC.get("requestId");
        logger.info(FLOW, "Handling /hello request [{}]", requestId);

        String greeting = helloService.hello();

        String port = env.getProperty("local.server.port");
        if (port == null) {
            port = env.getProperty("server.port", "8081");
        }

        HelloResponse resp = new HelloResponse(greeting, port);
        logger.debug(FLOW, "/hello response [{}] -> {}", requestId, resp);
        return resp;
    }
}
