package com.upc.pre.peaceapp.notifications.interfaces.rest.resources;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;

import java.time.Instant;

public record NotificationResource(Long id, NotificationKind kind, String title, String body, Long referenceId,
                                   boolean read, Instant createdAt, Instant readAt) {
}
