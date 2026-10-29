package com.upc.pre.peaceapp.reports.domain.model.events;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

/**
 * Evento de dominio publicado cuando se crea un reporte. El contexto de alertas reacciona
 * avisando a los usuarios que están cerca del incidente.
 */
public record ReportCreatedEvent(Long reportId, Long reporterId, String title, ReportCategory category,
                                 ReportSeverity severity, GeoPoint location) {
}
