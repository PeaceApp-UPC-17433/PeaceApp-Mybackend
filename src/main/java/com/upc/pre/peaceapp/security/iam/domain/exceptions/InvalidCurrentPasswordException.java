package com.upc.pre.peaceapp.security.iam.domain.exceptions;

public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
        super("La contraseña actual no es correcta");
    }
}
