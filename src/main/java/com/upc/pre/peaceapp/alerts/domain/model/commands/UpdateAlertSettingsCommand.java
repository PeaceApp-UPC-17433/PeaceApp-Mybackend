package com.upc.pre.peaceapp.alerts.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

import java.time.Instant;
import java.util.Set;

public record UpdateAlertSettingsCommand(String requesterEmail, int radiusMeters, ReportSeverity minSeverity,
                                         Set<ReportCategory> ignoredTypes, Instant mutedUntil) {
}
