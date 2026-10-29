package com.upc.pre.peaceapp.alerts.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

/** Reemplaza toda la configuración: {@code ignoredTypes} o {@code mutedUntil} ausentes significan "ninguno". */
public record UpdateAlertSettingsResource(
        @NotNull(message = "El radio es obligatorio")
        @Min(value = 100, message = "El radio mínimo es 100 metros")
        @Max(value = 20000, message = "El radio máximo es 20000 metros") Integer radiusMeters,
        @NotNull(message = "La severidad mínima es obligatoria") ReportSeverity minSeverity,
        Set<ReportCategory> ignoredTypes,
        Instant mutedUntil) {
}
