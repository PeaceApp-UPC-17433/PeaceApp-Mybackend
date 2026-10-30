package com.upc.pre.peaceapp.profile;

/** Se publica dentro de la transacción que borra el perfil, para que otros contextos limpien sus datos. */
public record ProfileDeletedEvent(Long profileId) {
}
