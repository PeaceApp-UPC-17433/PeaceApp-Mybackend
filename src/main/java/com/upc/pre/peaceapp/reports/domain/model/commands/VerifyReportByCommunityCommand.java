package com.upc.pre.peaceapp.reports.domain.model.commands;

/** Orden del sistema (no de un usuario): la comunidad confirmó el reporte. */
public record VerifyReportByCommunityCommand(Long reportId) {
}
