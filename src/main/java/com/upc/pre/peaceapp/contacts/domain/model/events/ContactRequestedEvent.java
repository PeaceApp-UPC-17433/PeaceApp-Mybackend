package com.upc.pre.peaceapp.contacts.domain.model.events;

/** Se publica al enviar una solicitud; notificaciones avisa al destinatario. */
public record ContactRequestedEvent(Long contactId, Long requesterId, Long addresseeId, String requesterName) {
}
