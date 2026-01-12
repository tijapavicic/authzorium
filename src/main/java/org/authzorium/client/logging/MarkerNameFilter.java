package org.authzorium.client.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Configurable filter that matches a specific marker name. If the marker is present and matches,
 * the filter will ACCEPT or DENY depending on {@code acceptOnMatch} property. Otherwise returns NEUTRAL.
 */
public class MarkerNameFilter extends Filter<ILoggingEvent> {

    private String markerName;
    private boolean acceptOnMatch = true;

    public void setMarkerName(String markerName) {
        this.markerName = markerName;
    }

    public void setAcceptOnMatch(boolean acceptOnMatch) {
        this.acceptOnMatch = acceptOnMatch;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null || markerName == null) return FilterReply.NEUTRAL;
        Marker marker = event.getMarker();
        if (marker == null) return FilterReply.NEUTRAL;
        boolean matches = markerName.equals(marker.getName()) || marker.contains(markerName);
        if (matches) {
            return acceptOnMatch ? FilterReply.ACCEPT : FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}

