package com.upc.pre.peaceapp.contacts.application.internal.eventhandlers;

import com.upc.pre.peaceapp.contacts.domain.services.ContactCommandService;
import com.upc.pre.peaceapp.profile.ProfileDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al borrar un perfil, sus relaciones se eliminan en la misma transacción. */
@Component
public class ProfileDeletedEventHandler {
    private final ContactCommandService commandService;

    public ProfileDeletedEventHandler(ContactCommandService commandService) {
        this.commandService = commandService;
    }

    @EventListener
    public void on(ProfileDeletedEvent event) {
        commandService.purgeProfile(event.profileId());
    }
}
