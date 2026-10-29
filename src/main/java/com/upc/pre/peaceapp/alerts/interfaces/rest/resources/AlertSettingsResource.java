package com.upc.pre.peaceapp.alerts.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

import java.time.Instant;
import java.util.Set;

/** {@code latitude}, {@code longitude} y {@code locationUpdatedAt} son nulos hasta que el usuario envía su ubicación. */
public record AlertSettingsResource(int radiusMeters, ReportSeverity minSeverity, Set<ReportCategory> ignoredTypes,
                                    Instant mutedUntil, Double latitude, Double longitude,
                                    Instant locationUpdatedAt) {
}
