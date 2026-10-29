package com.upc.pre.peaceapp.notifications.infrastructure.push;

import com.upc.pre.peaceapp.notifications.application.internal.outboundservices.push.PushNotificationSender;
import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Solo para desarrollo: no envía nada, deja constancia en el log. */
@Component
public class LoggingPushNotificationSender implements PushNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingPushNotificationSender.class);

    @Override
    public void send(String deviceToken, DevicePlatform platform, String title, String body) {
        log.info("[push {}] dispositivo ...{}: {} - {}", platform,
                deviceToken.substring(Math.max(0, deviceToken.length() - 6)), title, body);
    }
}
