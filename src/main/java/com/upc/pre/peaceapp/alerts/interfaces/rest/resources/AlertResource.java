package com.upc.pre.peaceapp.alerts.interfaces.rest.resources;

import com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

import java.time.Instant;

public record AlertResource(Long id, Long reportId, Long recipientId, String title, ReportCategory type,
                            ReportSeverity severity, double latitude, double longitude, int distanceMeters,
                            AlertStatus status, Instant createdAt, Instant readAt) {
}
