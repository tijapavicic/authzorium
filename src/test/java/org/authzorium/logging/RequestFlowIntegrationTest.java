package org.authzorium.logging;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test that starts the full Spring context and makes a real MockMvc call to /hello.
 * It verifies both the dedicated request-flow file and the standard application log file contain
 * the logged message from the controller/service which uses the FLOW marker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localh2,test")
@TestExecutionListeners(listeners = {LoggingTestExecutionListener.class, DependencyInjectionTestExecutionListener.class}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class RequestFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // use temp dir created by LoggingTestExecutionListener
    private Path tempLogDir() {
        return LoggingTestExecutionListener.getTempLogDir();
    }

    @Test
    @Disabled
    void whenRequestingHello_thenLogsAreWrittenToBothFiles() throws Exception {
        mockMvc.perform(get("/hello")).andExpect(status().isOk());

        Path td = tempLogDir();
        if (td == null) {
            fail("LoggingTestExecutionListener did not initialize the temp log directory; ensure the listener is registered and runs before the Spring context boots.");
        }

        Path flowFile = td.resolve("request-flow.log");
        Path appFile = td.resolve("application.log");

        // Wait for files to be created (log IO can be async); timeout after 10 seconds
        waitForFile(flowFile, 10_000);
        waitForFile(appFile, 10_000);

        String flow = Files.readString(flowFile);
        String app = Files.readString(appFile);

        assertThat(flow).contains("Handling /hello request");
        assertThat(app).contains("Handling /hello request");
    }

    private static void waitForFile(Path p, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        long sleep = 25;
        while (!Files.exists(p)) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                fail("Timed out waiting for file to appear: " + p);
            }
            Thread.sleep(sleep); // small polling delay; acceptable in tests
            // exponential backoff up to 200ms
            sleep = Math.min(200, sleep * 2);
        }
        // ensure it is readable
        long start2 = System.currentTimeMillis();
        while (true) {
            try {
                if (Files.size(p) >= 0) return;
            } catch (Exception e) {
                // retry until timeout
            }
            if (System.currentTimeMillis() - start2 > timeoutMs) {
                fail("Timed out waiting for file to become readable: " + p);
            }
            Thread.sleep(50);
        }
    }
}
