package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeReportStatusResource(
        @NotNull(message = "El estado es obligatorio")
        ReportStatus status) {
}
