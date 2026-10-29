package com.upc.pre.peaceapp.notifications.interfaces.rest.resources;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;

public record DeviceResource(Long id, DevicePlatform platform) {
}
