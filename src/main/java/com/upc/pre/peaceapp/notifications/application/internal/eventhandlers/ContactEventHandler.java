package com.upc.pre.peaceapp.notifications.application.internal.eventhandlers;

import com.upc.pre.peaceapp.contacts.domain.model.events.ContactAcceptedEvent;
import com.upc.pre.peaceapp.contacts.domain.model.events.ContactRequestedEvent;
import com.upc.pre.peaceapp.notifications.domain.model.commands.CreateNotificationCommand;
import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Avisa a las personas involucradas cuando cambia una relación de confianza. Un fallo se registra pero no se propaga. */
@Component
public class ContactEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ContactEventHandler.class);

    private final NotificationCommandService commandService;

    public ContactEventHandler(NotificationCommandService commandService) {
        this.commandService = commandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ContactRequestedEvent event) {
        notify(event.addresseeId(), NotificationKind.CONTACT_REQUEST, "Nueva solicitud de contacto",
                "%s quiere agregarte como contacto de confianza".formatted(event.requesterName()), event.contactId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ContactAcceptedEvent event) {
        notify(event.requesterId(), NotificationKind.CONTACT_ACCEPTED, "Solicitud aceptada",
                "%s aceptó tu solicitud de contacto de confianza".formatted(event.addresseeName()), event.contactId());
    }

    private void notify(Long recipientId, NotificationKind kind, String title, String body, Long contactId) {
        try {
            commandService.handle(new CreateNotificationCommand(recipientId, kind, title, body, contactId));
        } catch (RuntimeException ex) {
            log.error("No se pudo notificar el contacto {}", contactId, ex);
        }
    }
}
