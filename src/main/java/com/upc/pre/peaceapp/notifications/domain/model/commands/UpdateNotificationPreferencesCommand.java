package com.upc.pre.peaceapp.notifications.domain.model.commands;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;

import java.util.Set;

public record UpdateNotificationPreferencesCommand(String requesterEmail, boolean inAppEnabled, boolean pushEnabled,
                                                   Set<NotificationKind> disabledKinds) {
}
