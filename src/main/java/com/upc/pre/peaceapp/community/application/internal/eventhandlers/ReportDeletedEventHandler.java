package com.upc.pre.peaceapp.community.application.internal.eventhandlers;

import com.upc.pre.peaceapp.community.domain.model.commands.PurgeReportCommunityDataCommand;
import com.upc.pre.peaceapp.community.domain.services.CommunityCleanupCommandService;
import com.upc.pre.peaceapp.reports.domain.model.events.ReportDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Se ejecuta de forma síncrona dentro de la transacción que borra el reporte:
 * si falla la limpieza, el reporte tampoco se borra y no quedan comentarios ni votos huérfanos.
 */
@Component
public class ReportDeletedEventHandler {
    private final CommunityCleanupCommandService cleanupService;

    public ReportDeletedEventHandler(CommunityCleanupCommandService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @EventListener
    public void on(ReportDeletedEvent event) {
        cleanupService.handle(new PurgeReportCommunityDataCommand(event.reportId()));
    }
}
