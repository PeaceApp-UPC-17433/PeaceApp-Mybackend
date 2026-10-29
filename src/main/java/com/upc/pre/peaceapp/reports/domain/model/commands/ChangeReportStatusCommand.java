package com.upc.pre.peaceapp.reports.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;

public record ChangeReportStatusCommand(Long reportId, boolean admin, ReportStatus newStatus) {
}
