package com.upc.pre.peaceapp.alerts.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.*;

public record GenerateProximityAlertsCommand(Long reportId, Long reporterId, String title, ReportCategory category, ReportSeverity severity, GeoPoint location) {
}
