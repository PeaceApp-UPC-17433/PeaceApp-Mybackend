package com.upc.pre.peaceapp.security.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code username} es el correo del usuario; se conserva el nombre del campo por compatibilidad con el frontend. */
public record SignInResource(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}
