package com.upc.pre.peaceapp.alerts.domain.exceptions;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException() {
        super("La alerta no existe");
    }
}
