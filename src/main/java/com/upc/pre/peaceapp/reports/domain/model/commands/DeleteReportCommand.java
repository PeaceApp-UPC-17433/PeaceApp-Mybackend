package com.upc.pre.peaceapp.reports.domain.model.commands;

public record DeleteReportCommand(Long reportId, String requesterEmail, boolean admin) {
}
