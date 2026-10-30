package com.upc.pre.peaceapp.community.domain.exceptions;

public class AbuseFlagAlreadyResolvedException extends RuntimeException {
    public AbuseFlagAlreadyResolvedException() {
        super("La denuncia ya fue resuelta");
    }
}
