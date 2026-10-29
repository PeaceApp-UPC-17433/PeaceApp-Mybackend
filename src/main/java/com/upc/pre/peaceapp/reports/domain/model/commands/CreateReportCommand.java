package com.upc.pre.peaceapp.reports.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

import java.time.Instant;

public record CreateReportCommand(String requesterEmail, String title, String detail, ReportCategory category,
                                  ReportSeverity severity, Instant occurredAt, GeoPoint location,
                                  String address, String image) {
}
