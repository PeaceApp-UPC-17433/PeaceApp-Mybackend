package com.upc.pre.peaceapp.security.iam.domain.model.events;

/**
 * Evento de dominio publicado por IAM cuando se crea una cuenta.
 * El contexto de perfiles reacciona creando el perfil asociado.
 */
public record UserRegisteredEvent(Long userId, String email, String name, String lastname, String phonenumber) {
}
