package com.upc.pre.peaceapp.notifications.domain.services;

import com.upc.pre.peaceapp.notifications.domain.model.aggregates.Notification;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences;
import com.upc.pre.peaceapp.notifications.domain.model.queries.*;
import org.springframework.data.domain.Page;

public interface NotificationQueryService {
    Page<Notification> handle(GetNotificationsQuery query);

    long handle(CountUnreadNotificationsQuery query);

    /** Si el usuario no configuró nada, devuelve los valores por defecto sin guardarlos. */
    NotificationPreferences handle(GetNotificationPreferencesQuery query);
}
