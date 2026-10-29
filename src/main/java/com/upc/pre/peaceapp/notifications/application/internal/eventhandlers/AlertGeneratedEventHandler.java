package com.upc.pre.peaceapp.notifications.application.internal.eventhandlers;

import com.upc.pre.peaceapp.alerts.domain.model.events.AlertGeneratedEvent;
import com.upc.pre.peaceapp.notifications.domain.model.commands.CreateNotificationCommand;
import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Convierte cada alerta nueva en un aviso para el usuario. Un fallo se registra pero no se propaga. */
@Component
public class AlertGeneratedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AlertGeneratedEventHandler.class);

    private final NotificationCommandService commandService;

    public AlertGeneratedEventHandler(NotificationCommandService commandService) {
        this.commandService = commandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AlertGeneratedEvent event) {
        try {
            commandService.handle(new CreateNotificationCommand(event.recipientId(), NotificationKind.PROXIMITY_ALERT,
                    "Incidente cerca de ti",
                    "%s, a %d m de tu ubicación".formatted(event.reportTitle(), event.distanceMeters()),
                    event.alertId()));
        } catch (RuntimeException ex) {
            log.error("No se pudo notificar la alerta {}", event.alertId(), ex);
        }
    }
}
