package com.upc.pre.peaceapp.notifications.domain.model.commands;

public record MarkNotificationReadCommand(Long notificationId, String requesterEmail) {
}
