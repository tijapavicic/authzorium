package org.authzorium.client.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Simple test that exercises "FLOW" marker logging and verifies the request-flow file contains the entry.
 *
 * This test sets a temporary LOG_HOME to avoid writing into the project logs directory.
 */
class RequestFlowLoggingTest {

    private Path tempLogDir;

    @BeforeEach
    void setUp() throws IOException {
        tempLogDir = Files.createTempDirectory("request-flow-test-logs");
        // point the logback config to write into the temp dir
        System.setProperty("LOG_HOME", tempLogDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // clear property
        System.clearProperty("LOG_HOME");
        if (tempLogDir != null && Files.exists(tempLogDir)) {
            try (Stream<Path> s = Files.walk(tempLogDir)) {
                s.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                // best-effort cleanup in tests
                            }
                        });
            }
        }
    }

    @Test
    void whenLoggingWithFlowMarker_thenRequestFlowFileContainsEntry() throws Exception {
        // ensure logging subsystem is initialized with our LOG_HOME by reloading the Logback config explicitly
        LoggerContext ctx = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        ctx.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(ctx);
        try (var is = getClass().getResourceAsStream("/logback-spring.xml")) {
            if (is == null) {
                throw new IllegalStateException("logback-spring.xml not found on classpath");
            }
            configurator.doConfigure(is);
        }
        StatusPrinter.printIfErrorsOccured(ctx);

        var testLogger = org.slf4j.LoggerFactory.getLogger("org.loket.authN.flow.Test");
        Marker flow = MarkerFactory.getMarker("FLOW");

        String message = "Flow test entry - unique:" + System.nanoTime();
        // use INFO level so root INFO doesn't filter it out
        testLogger.info(flow, message);

        // Wait for the request-flow file to be written
        Path flowFile = tempLogDir.resolve("request-flow.log");
        waitForFile(flowFile, 5000);

        assertThat(Files.exists(flowFile)).isTrue();
        String content = Files.readString(flowFile);
        assertThat(content).contains(message);
    }

    private static void waitForFile(Path p, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        long sleep = 25;
        while (!Files.exists(p)) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                fail("Timed out waiting for file to appear: " + p);
            }
            Thread.sleep(sleep);
            sleep = Math.min(200, sleep * 2);
        }
        // ensure readable
        long start2 = System.currentTimeMillis();
        while (true) {
            try {
                if (Files.size(p) >= 0) return;
            } catch (Exception ignored) {
            }
            if (System.currentTimeMillis() - start2 > timeoutMs) {
                fail("Timed out waiting for file to become readable: " + p);
            }
            Thread.sleep(50);
        }
    }
}
