package com.upc.pre.peaceapp.notifications.interfaces.rest.resources;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** Reemplaza todas las preferencias; {@code disabledKinds} ausente significa "ninguno desactivado". */
public record UpdateNotificationPreferencesResource(
        @NotNull(message = "inAppEnabled es obligatorio") Boolean inAppEnabled,
        @NotNull(message = "pushEnabled es obligatorio") Boolean pushEnabled,
        Set<NotificationKind> disabledKinds) {
}
