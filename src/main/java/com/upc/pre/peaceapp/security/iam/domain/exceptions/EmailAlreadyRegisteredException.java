package com.upc.pre.peaceapp.security.iam.domain.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("El correo " + email + " ya está registrado");
    }
}
