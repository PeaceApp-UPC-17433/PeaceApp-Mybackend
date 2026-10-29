package com.upc.pre.peaceapp.schemas;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Actualización parcial: un campo {@code null} se deja sin cambios. */
public record UpdateUserProfileSchema(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
        String name,

        @Pattern(regexp = ".*\\S.*", message = "El apellido no puede estar vacío")
        @Size(max = 50, message = "El apellido no puede superar 50 caracteres")
        String lastname,

        @Pattern(regexp = "^$|^[0-9+\\- ]{6,20}$", message = "El teléfono no tiene un formato válido")
        String phonenumber,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 50, message = "El correo no puede superar 50 caracteres")
        String email,

        @Size(max = 500, message = "La URL de la imagen es demasiado larga")
        String profile_image
) {
}
