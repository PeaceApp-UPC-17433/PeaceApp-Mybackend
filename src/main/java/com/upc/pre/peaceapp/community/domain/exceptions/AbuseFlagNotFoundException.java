package com.upc.pre.peaceapp.community.domain.exceptions;

public class AbuseFlagNotFoundException extends RuntimeException {
    public AbuseFlagNotFoundException() {
        super("La denuncia no existe");
    }
}
