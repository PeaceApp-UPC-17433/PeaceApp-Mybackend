package com.upc.pre.peaceapp.profile;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Perfil no encontrado");
    }
}
