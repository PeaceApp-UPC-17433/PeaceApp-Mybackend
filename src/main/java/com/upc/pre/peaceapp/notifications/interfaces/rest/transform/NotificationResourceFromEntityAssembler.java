package com.upc.pre.peaceapp.notifications.interfaces.rest.transform;

import com.upc.pre.peaceapp.notifications.domain.model.aggregates.Notification;
import com.upc.pre.peaceapp.notifications.interfaces.rest.resources.NotificationResource;

public class NotificationResourceFromEntityAssembler {
    public static NotificationResource toResourceFromEntity(Notification notification) {
        return new NotificationResource(notification.getId(), notification.getKind(), notification.getTitle(),
                notification.getBody(), notification.getReferenceId(), notification.isRead(),
                notification.getCreatedAt().toInstant(), notification.getReadAt());
    }
}
