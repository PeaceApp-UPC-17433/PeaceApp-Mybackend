package com.upc.pre.peaceapp.contacts.domain.services;

import com.upc.pre.peaceapp.contacts.domain.model.aggregates.TrustedContact;
import com.upc.pre.peaceapp.contacts.domain.model.commands.*;

public interface ContactCommandService {
    /** Envía una solicitud por correo. Falla si ya hay una relación con esa persona. */
    TrustedContact handle(RequestContactCommand command);

    TrustedContact handle(AcceptContactCommand command);

    /** Rechazar elimina la solicitud: quien la envió puede volver a intentarlo. */
    void handle(DeclineContactCommand command);

    TrustedContact handle(BlockContactCommand command);

    /** Quita un contacto, cancela una solicitud propia o deshace un bloqueo propio. */
    void handle(RemoveContactCommand command);

    /** Elimina todas las relaciones de un perfil que ya no existe. */
    void purgeProfile(Long profileId);
}
