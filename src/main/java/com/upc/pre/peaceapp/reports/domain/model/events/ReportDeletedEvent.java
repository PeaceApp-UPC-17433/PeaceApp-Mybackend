package com.upc.pre.peaceapp.reports.domain.model.events;

/** Evento de dominio publicado al borrar un reporte. Comunidad reacciona limpiando comentarios, votos y denuncias. */
public record ReportDeletedEvent(Long reportId) {
}
