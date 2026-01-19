package org.authzorium.client.view;

// Minimal placeholder for owner view so code compiles when Blaze is not present on the classpath
public interface OwnerView {
    Long getId();
    String getUsername();
    String getDisplayName();
}
