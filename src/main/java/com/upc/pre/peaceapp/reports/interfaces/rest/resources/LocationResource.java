package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

/** Ubicación de un reporte; {@code id} es el id del reporte. */
public record LocationResource(Long id, double latitude, double longitude, Long idReport) {
}
