package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code resolution}: {@code DISMISSED} o {@code ACTION_TAKEN}. */
public record ResolveAbuseFlagResource(
        @NotNull(message = "La resolución es obligatoria") FlagStatus resolution,
        @Size(max = 255, message = "La nota no puede superar 255 caracteres") String note) {
}
