package com.upc.pre.peaceapp.community.domain.exceptions;

public class DuplicateAbuseFlagException extends RuntimeException {
    public DuplicateAbuseFlagException() {
        super("Ya denunciaste este contenido");
    }
}
