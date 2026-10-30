package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FlagContentResource(
        @NotNull(message = "El motivo es obligatorio") FlagReason reason,
        @Size(max = 255, message = "El detalle no puede superar 255 caracteres") String details) {
}
