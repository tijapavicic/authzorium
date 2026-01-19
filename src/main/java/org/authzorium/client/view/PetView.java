package org.authzorium.client.view;

// Placeholder interface for a Pet view. Keep minimal to avoid requiring Blaze on the compile classpath.
public interface PetView {
    Long getId();
    String getPetName();
    OwnerView getOwner();
}
