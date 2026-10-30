package com.upc.pre.peaceapp.contacts.domain.model.events;

/** Se publica al aceptar una solicitud; notificaciones avisa a quien la envió. */
public record ContactAcceptedEvent(Long contactId, Long requesterId, Long addresseeId, String addresseeName) {
}
