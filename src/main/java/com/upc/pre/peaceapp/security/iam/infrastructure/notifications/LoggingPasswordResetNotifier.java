package com.upc.pre.peaceapp.security.iam.infrastructure.notifications;

import com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.notifications.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación provisional: solo registra el token en el log.
 * Sustituir por un adaptador SMTP antes de producción (el token en logs es sensible).
 */
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void sendResetToken(String email, String rawToken) {
        LOGGER.warn("[SOLO DESARROLLO] Token de recuperación para {}: {}", email, rawToken);
    }
}
