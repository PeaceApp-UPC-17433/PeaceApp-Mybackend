package com.upc.pre.peaceapp.notifications.domain.model.commands;

public record DeleteNotificationCommand(Long notificationId, String requesterEmail) {
}
