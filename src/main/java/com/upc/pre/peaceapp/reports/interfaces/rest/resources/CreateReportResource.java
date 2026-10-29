package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record CreateReportResource(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 50, message = "El título no puede superar 50 caracteres")
        String title,

        @Size(max = 255, message = "El detalle no puede superar 255 caracteres")
        String detail,

        @NotNull(message = "El tipo es obligatorio")
        ReportCategory type,

        ReportSeverity severity,

        Instant occurredAt,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
        Double longitude,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 100, message = "La dirección no puede superar 100 caracteres")
        String address,

        @Size(max = 500, message = "La imagen no puede superar 500 caracteres")
        String image) {
}
