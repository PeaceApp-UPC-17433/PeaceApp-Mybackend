package com.upc.pre.peaceapp.notifications.interfaces.rest.resources;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;

import java.util.Set;

public record NotificationPreferencesResource(boolean inAppEnabled, boolean pushEnabled,
                                              Set<NotificationKind> disabledKinds) {
}
