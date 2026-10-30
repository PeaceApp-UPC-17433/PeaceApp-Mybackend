package com.upc.pre.peaceapp.community.domain.model.valueobjects;

/** {@code OPEN} espera revisión; los otros dos son resoluciones finales. */
public enum FlagStatus {
    OPEN, DISMISSED, ACTION_TAKEN;

    public boolean isResolution() {
        return this != OPEN;
    }
}
