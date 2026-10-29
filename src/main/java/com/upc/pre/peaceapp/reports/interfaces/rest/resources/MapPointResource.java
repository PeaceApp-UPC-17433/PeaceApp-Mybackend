package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

/** Lo mínimo que necesita el mapa para pintar un marcador. */
public record MapPointResource(Long id, double latitude, double longitude, ReportCategory type,
                               ReportSeverity severity) {
}
