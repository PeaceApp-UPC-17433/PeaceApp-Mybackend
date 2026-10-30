package com.upc.pre.peaceapp.community.domain.model.events;

/**
 * Evento de dominio: las confirmaciones netas de un reporte alcanzaron el umbral. El contexto de reportes
 * reacciona marcándolo como verificado.
 */
public record ReportConfirmedByCommunityEvent(Long reportId, long netConfirmations) {
}
