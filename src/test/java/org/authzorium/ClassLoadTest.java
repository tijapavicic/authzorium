package org.authzorium;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassLoadTest {

    @Test
    void canLoadHelloServiceAndImpl() throws Exception {
        Class<?> iface = Class.forName("org.authzorium.client.service.HelloService");
        assertNotNull(iface);

        Class<?> impl = Class.forName("org.authzorium.client.service.impl.HelloServiceImpl");
        assertNotNull(impl);
    }
}

