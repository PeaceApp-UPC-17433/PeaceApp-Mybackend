package com.upc.pre.peaceapp.contacts.domain.model.valueobjects;

/** Estado de la relación entre dos usuarios. Rechazar una solicitud la elimina, por eso no hay estado para eso. */
public enum ContactStatus {
    PENDING, ACCEPTED, BLOCKED
}
