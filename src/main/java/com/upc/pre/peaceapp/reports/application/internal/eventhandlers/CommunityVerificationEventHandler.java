package com.upc.pre.peaceapp.reports.application.internal.eventhandlers;

import com.upc.pre.peaceapp.community.domain.model.events.ReportConfirmedByCommunityEvent;
import com.upc.pre.peaceapp.reports.domain.model.commands.VerifyReportByCommunityCommand;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Marca como verificado el reporte que la comunidad confirmó. Corre tras confirmar el voto y en su propia
 * transacción: si falla, el voto ya guardado no se pierde y el error queda en el log.
 */
@Component
public class CommunityVerificationEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CommunityVerificationEventHandler.class);

    private final ReportCommandService commandService;

    public CommunityVerificationEventHandler(ReportCommandService commandService) {
        this.commandService = commandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ReportConfirmedByCommunityEvent event) {
        try {
            commandService.handle(new VerifyReportByCommunityCommand(event.reportId()));
        } catch (RuntimeException ex) {
            log.error("No se pudo verificar el reporte {} tras la confirmación de la comunidad", event.reportId(), ex);
        }
    }
}
