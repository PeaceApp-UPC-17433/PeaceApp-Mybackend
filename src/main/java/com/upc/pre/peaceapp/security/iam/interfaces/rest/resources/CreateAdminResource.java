package com.upc.pre.peaceapp.security.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminResource(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 50, message = "El correo no puede superar 50 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
        String name,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 50, message = "El apellido no puede superar 50 caracteres")
        String lastname) {
}
