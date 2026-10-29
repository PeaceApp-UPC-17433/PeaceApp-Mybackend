package com.upc.pre.peaceapp.alerts.domain.model.events;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;

/** Se publica por cada alerta creada; el contexto de notificaciones la convierte en aviso para el usuario. */
public record AlertGeneratedEvent(Long alertId, Long recipientId, Long reportId, String reportTitle,
                                  ReportCategory reportType, ReportSeverity reportSeverity, int distanceMeters) {
}
