package org.authzorium.logging;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * TestExecutionListener that sets a temporary LOG_HOME before the Spring Test context starts
 * and cleans it up after the test class finishes. This ensures Logback picks up the test LOG_HOME
 * during initialization (avoids programmatic reconfiguration in tests).
 */
public class LoggingTestExecutionListener implements TestExecutionListener {

    private static Path tempLogDir;

    public static Path getTempLogDir() {
        return tempLogDir;
    }

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        tempLogDir = Files.createTempDirectory("request-flow-int-test-logs");
        System.setProperty("LOG_HOME", tempLogDir.toString());
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        System.clearProperty("LOG_HOME");
        if (tempLogDir != null && Files.exists(tempLogDir)) {
            try (Stream<Path> s = Files.walk(tempLogDir)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    // other TestExecutionListener methods are no-ops for our needs
}

