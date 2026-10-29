package com.upc.pre.peaceapp.eventhandlers;

import com.upc.pre.peaceapp.security.iam.domain.model.events.UserRegisteredEvent;
import com.upc.pre.peaceapp.services.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Se ejecuta de forma síncrona dentro de la transacción del registro:
 * si falla la creación del perfil, tampoco se crea la cuenta.
 */
@Component
public class UserRegisteredEventHandler {
    private final UserService userService;

    public UserRegisteredEventHandler(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void on(UserRegisteredEvent event) {
        userService.createProfile(event);
    }
}
