package com.upc.pre.peaceapp.notifications.application.internal.outboundservices.push;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;

/**
 * Puerto de salida hacia el proveedor de notificaciones push. Mientras no haya un adaptador real
 * (por ejemplo Firebase Cloud Messaging), la implementación solo escribe en el log.
 */
public interface PushNotificationSender {
    void send(String deviceToken, DevicePlatform platform, String title, String body);
}
