package org.authzorium.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Simple Logback filter that accepts logging events which contain the Marker 'FLOW'.
 * This avoids relying on the logback-classic MarkerFilter class being present on the runtime classpath.
 */
public class FlowMarkerFilter extends Filter<ILoggingEvent> {

    private static final String FLOW_MARKER = "FLOW";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) return FilterReply.DENY;
        Marker marker = event.getMarker();
        if (marker == null) return FilterReply.DENY;
        // Marker.contains is supported by SLF4J markers
        if (marker.contains(FLOW_MARKER) || FLOW_MARKER.equals(marker.getName())) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }
}

