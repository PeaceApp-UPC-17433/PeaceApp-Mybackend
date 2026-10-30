package com.upc.pre.peaceapp.contacts.domain.exceptions;

public class ContactNotFoundException extends RuntimeException {
    public ContactNotFoundException() {
        super("Contacto no encontrado");
    }
}
