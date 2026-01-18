package org.authzorium.client.util;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class LoggingConstants {
    private LoggingConstants() {}

    public static final String MDC_REQUEST_ID = "requestId";
    public static final Marker flow = MarkerFactory.getMarker("FLOW");
}
