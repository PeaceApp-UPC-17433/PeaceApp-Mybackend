package com.upc.pre.peaceapp.security.iam.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electrónico normalizado (sin espacios y en minúsculas).
 * Es el identificador de inicio de sesión del usuario.
 */
public record EmailAddress(String value) {
    public static final int MAX_LENGTH = 50;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El correo es obligatorio");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches())
            throw new IllegalArgumentException("El correo no tiene un formato válido");
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}
