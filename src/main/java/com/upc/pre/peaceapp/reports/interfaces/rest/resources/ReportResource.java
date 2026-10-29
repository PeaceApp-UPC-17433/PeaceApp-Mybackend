package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;

import java.time.Instant;
import java.util.Date;

public record ReportResource(Long id, String title, String detail, ReportCategory type, ReportSeverity severity,
                             ReportStatus status, Instant occurredAt, double latitude, double longitude,
                             String address, String image, Long idUser, Date createdAt, Date updatedAt) {
}
