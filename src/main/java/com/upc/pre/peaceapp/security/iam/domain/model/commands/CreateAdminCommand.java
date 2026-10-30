package com.upc.pre.peaceapp.security.iam.domain.model.commands;

/** Un administrador autenticado crea otra cuenta con ROLE_ADMIN. */
public record CreateAdminCommand(String email, String password, String name, String lastname) {
}
