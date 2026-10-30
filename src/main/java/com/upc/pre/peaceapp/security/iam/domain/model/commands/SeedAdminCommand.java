package com.upc.pre.peaceapp.security.iam.domain.model.commands;

/** Crea la cuenta de administrador inicial si todavía no existe. Se usa solo al arrancar la aplicación. */
public record SeedAdminCommand(String email, String password, String name, String lastname) {
}
