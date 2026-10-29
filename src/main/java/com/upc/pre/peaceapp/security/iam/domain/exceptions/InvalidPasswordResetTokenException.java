package com.upc.pre.peaceapp.security.iam.domain.exceptions;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("El token de recuperación es inválido o ha expirado");
    }
}
