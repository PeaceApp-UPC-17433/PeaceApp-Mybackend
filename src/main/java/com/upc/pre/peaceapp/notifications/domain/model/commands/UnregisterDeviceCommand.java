package com.upc.pre.peaceapp.notifications.domain.model.commands;

public record UnregisterDeviceCommand(String requesterEmail, String token) {
}
