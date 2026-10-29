package com.upc.pre.peaceapp.notifications.domain.exceptions;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("La notificación no existe");
    }
}
