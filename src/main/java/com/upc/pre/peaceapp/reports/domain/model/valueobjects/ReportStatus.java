package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida de un reporte. {@code DISCARDED} y {@code RESOLVED} son finales. */
public enum ReportStatus {
    PENDING, UNDER_REVIEW, VERIFIED, DISCARDED, RESOLVED;

    public boolean canTransitionTo(ReportStatus next) {
        return allowedNext().contains(next);
    }

    /** Un reporte activo cuenta para el mapa de calor. */
    public boolean isActive() {
        return this != DISCARDED && this != RESOLVED;
    }

    private Set<ReportStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(UNDER_REVIEW, VERIFIED, DISCARDED);
            case UNDER_REVIEW -> EnumSet.of(VERIFIED, DISCARDED);
            case VERIFIED -> EnumSet.of(RESOLVED, DISCARDED);
            case DISCARDED, RESOLVED -> EnumSet.noneOf(ReportStatus.class);
        };
    }
}
