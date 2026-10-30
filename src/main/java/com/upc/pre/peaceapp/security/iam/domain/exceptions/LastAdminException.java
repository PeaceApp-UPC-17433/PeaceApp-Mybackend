package com.upc.pre.peaceapp.security.iam.domain.exceptions;

public class LastAdminException extends RuntimeException {
    public LastAdminException() {
        super("No se puede quitar el rol de administrador al último administrador");
    }
}
