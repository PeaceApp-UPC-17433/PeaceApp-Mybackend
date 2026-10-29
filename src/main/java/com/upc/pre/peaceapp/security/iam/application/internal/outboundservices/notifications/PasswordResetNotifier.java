package com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.notifications;

/**
 * Puerto de salida para entregar al usuario el token de recuperación (correo, SMS, etc.).
 */
public interface PasswordResetNotifier {
    void sendResetToken(String email, String rawToken);
}
