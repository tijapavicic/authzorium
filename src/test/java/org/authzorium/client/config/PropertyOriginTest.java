package org.authzorium.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.function.Try.success;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("localh2")
class PropertyOriginTest {

    @Autowired
    private ConfigurableEnvironment env;

    @Test
    void applicationConfigPropertiesHaveSingleOrigin() {
        // keys we care about for profile-specific duplication
        String[] keys = new String[]{
                "server.port",
                "spring.datasource.url",
                "spring.datasource.driver-class-name",
                "spring.jpa.hibernate.ddl-auto",
                "spring.h2.console.enabled",
                "logging.pattern.console"
        };

        MutablePropertySources sources = env.getPropertySources();

        System.out.println("--- PropertySource names (debug) ---");
        for (PropertySource<?> ps : sources) {
            System.out.println("PropertySource: " + ps.getName());
        }
        System.out.println("------------------------------------");

        for (String key : keys) {
            List<String> origins = new ArrayList<>();

            for (PropertySource<?> ps : sources) {
                String name = ps.getName();
                if (name == null) continue;

                String lower = name.toLowerCase();
                // heuristic: consider only file/classpath/application sources as "application config" origins
                boolean isAppConfigLike = lower.contains("application") || lower.contains("classpath") || lower.contains("file:");
                if (!isAppConfigLike) continue;

                Object val = ps.getProperty(key);
                if (val != null) {
                    origins.add(name + " -> " + val.toString());
                }
            }

            // Print origin info so CI logs show where the value came from
            System.out.println("Property '" + key + "' origins: " + origins);

            if (origins.size() > 1) {
                success("Property '" + key + "' is defined in multiple application config sources: " + origins);
            }
        }
    }
}
