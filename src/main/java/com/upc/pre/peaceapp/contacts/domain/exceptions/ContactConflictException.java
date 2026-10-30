package com.upc.pre.peaceapp.contacts.domain.exceptions;

/** La relación existente impide la operación pedida (solicitud repetida, ya son contactos, bloqueo). */
public class ContactConflictException extends RuntimeException {
    public ContactConflictException(String message) {
        super(message);
    }
}
