package com.upc.pre.peaceapp.reports.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

import java.time.Instant;

/** Edición parcial: los campos nulos no se modifican. */
public record UpdateReportCommand(Long reportId, String requesterEmail, boolean admin, String title, String detail,
                                  ReportCategory category, ReportSeverity severity, Instant occurredAt,
                                  GeoPoint location, String address, String image) {
}
