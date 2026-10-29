package com.upc.pre.peaceapp.notifications.domain.model.commands;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;

public record CreateNotificationCommand(Long recipientId, NotificationKind kind, String title, String body,
                                        Long referenceId) {
}
