package com.upc.pre.peaceapp.notifications.interfaces.rest.resources;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceResource(
        @NotBlank(message = "El token del dispositivo es obligatorio")
        @Size(max = 512, message = "El token del dispositivo es demasiado largo") String token,
        @NotNull(message = "La plataforma es obligatoria") DevicePlatform platform) {
}
