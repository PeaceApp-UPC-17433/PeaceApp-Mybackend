package com.upc.pre.peaceapp.notifications.application.internal.outboundservices.acl;

import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.services.UserService;
import org.springframework.stereotype.Service;

/** Capa anticorrupción hacia el contexto de usuarios: notificaciones solo conoce el id del perfil. */
@Service("notificationsExternalProfileService")
public class ExternalProfileService {
    private final UserService userService;

    public ExternalProfileService(UserService userService) {
        this.userService = userService;
    }

    public Long fetchProfileIdByEmail(String email) {
        return userService.findProfileIdByEmail(email).orElseThrow(ProfileNotFoundException::new);
    }
}
