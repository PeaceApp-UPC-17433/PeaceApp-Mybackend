package com.upc.pre.peaceapp.alerts.application.internal.eventhandlers;

import com.upc.pre.peaceapp.alerts.domain.model.commands.GenerateProximityAlertsCommand;
import com.upc.pre.peaceapp.alerts.domain.services.AlertCommandService;
import com.upc.pre.peaceapp.reports.domain.model.events.ReportCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Genera las alertas una vez confirmado el reporte. Un fallo aquí se registra pero no se propaga:
 * el reporte ya existe y el usuario que lo creó no debe ver un error por culpa de las alertas.
 */
@Component
public class ReportCreatedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ReportCreatedEventHandler.class);

    private final AlertCommandService commandService;

    public ReportCreatedEventHandler(AlertCommandService commandService) {
        this.commandService = commandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ReportCreatedEvent event) {
        try {
            var created = commandService.handle(new GenerateProximityAlertsCommand(event.reportId(),
                    event.reporterId(), event.title(), event.category(), event.severity(), event.location()));
            log.info("Reporte {}: {} alertas generadas", event.reportId(), created);
        } catch (RuntimeException ex) {
            log.error("No se pudieron generar las alertas del reporte {}", event.reportId(), ex);
        }
    }
}
