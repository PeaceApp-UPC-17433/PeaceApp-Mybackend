package com.upc.pre.peaceapp.notifications.domain.services;

import com.upc.pre.peaceapp.notifications.domain.model.aggregates.DeviceRegistration;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.Notification;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences;
import com.upc.pre.peaceapp.notifications.domain.model.commands.*;

import java.util.Optional;

public interface NotificationCommandService {
    /** Entrega el aviso por los canales que el usuario tenga activos; vacío si no se guardó nada en la app. */
    Optional<Notification> handle(CreateNotificationCommand command);

    Notification handle(MarkNotificationReadCommand command);

    /** Devuelve cuántas notificaciones pasaron a leídas. */
    int handle(MarkAllNotificationsReadCommand command);

    void handle(DeleteNotificationCommand command);

    NotificationPreferences handle(UpdateNotificationPreferencesCommand command);

    DeviceRegistration handle(RegisterDeviceCommand command);

    void handle(UnregisterDeviceCommand command);
}
