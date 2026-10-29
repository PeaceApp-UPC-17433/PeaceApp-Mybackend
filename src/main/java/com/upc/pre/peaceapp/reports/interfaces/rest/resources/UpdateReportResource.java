package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import jakarta.validation.constraints.*;

import java.time.Instant;

/** Edición parcial: los campos ausentes no se modifican. La latitud y la longitud van juntas. */
public record UpdateReportResource(
        @Size(min = 1, max = 50, message = "El título debe tener entre 1 y 50 caracteres")
        String title,

        @Size(max = 255, message = "El detalle no puede superar 255 caracteres")
        String detail,

        ReportCategory type,

        ReportSeverity severity,

        Instant occurredAt,

        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
        Double latitude,

        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
        Double longitude,

        @Size(min = 1, max = 100, message = "La dirección debe tener entre 1 y 100 caracteres")
        String address,

        @Size(max = 500, message = "La imagen no puede superar 500 caracteres")
        String image) {

    @JsonIgnore
    @AssertTrue(message = "La latitud y la longitud deben enviarse juntas")
    public boolean isCoordinatesComplete() {
        return (latitude == null) == (longitude == null);
    }
}
