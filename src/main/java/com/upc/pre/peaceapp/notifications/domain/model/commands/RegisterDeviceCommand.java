package com.upc.pre.peaceapp.notifications.domain.model.commands;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;

public record RegisterDeviceCommand(String requesterEmail, String token, DevicePlatform platform) {
}
