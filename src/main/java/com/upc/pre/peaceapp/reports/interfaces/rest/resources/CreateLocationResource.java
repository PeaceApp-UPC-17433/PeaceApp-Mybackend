package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import jakarta.validation.constraints.*;

public record CreateLocationResource(
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
        Double longitude,

        @NotNull(message = "El reporte es obligatorio")
        @Positive(message = "El reporte no es válido")
        Long idReport) {
}
