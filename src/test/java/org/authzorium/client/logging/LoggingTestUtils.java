package org.authzorium.client.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Small test utility helpers to make logging-related test setup reusable.
 * Other tests can call these helpers when they need to control LOG_HOME or
 * programmatically reload the Logback configuration.
 */
public final class LoggingTestUtils {

    private LoggingTestUtils() {
        // utility
    }

    /**
     * Returns the temp log directory created by the {@link LoggingTestExecutionListener}.
     * May be null if the listener wasn't used for the current test run.
     */
    public static Path getTempLogDirFromListener() {
        return LoggingTestExecutionListener.getTempLogDir();
    }

    /**
     * Programmatically reloads Logback using the classpath's `logback-spring.xml` so tests
     * that change LOG_HOME at runtime can ensure the logging system picks up the value.
     */
    public static void reloadLogbackFromClasspath() throws Exception {
        LoggerContext ctx = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        ctx.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(ctx);
        try (var is = LoggingTestUtils.class.getResourceAsStream("/logback-spring.xml")) {
            if (is == null) throw new IllegalStateException("logback-spring.xml not found on classpath");
            configurator.doConfigure(is);
        }
        StatusPrinter.printIfErrorsOccured(ctx);
    }

    /**
     * Best-effort delete directory tree used by tests.
     */
    public static void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // best-effort cleanup for tests
                        }
                    });
        }
    }
}

