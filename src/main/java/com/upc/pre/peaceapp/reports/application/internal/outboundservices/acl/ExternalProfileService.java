package com.upc.pre.peaceapp.reports.application.internal.outboundservices.acl;

import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.services.UserService;
import org.springframework.stereotype.Service;

/**
 * Capa anticorrupción hacia el contexto de usuarios: el contexto de reportes solo conoce
 * el id del perfil, nunca sus entidades ni su repositorio.
 */
@Service
public class ExternalProfileService {
    private final UserService userService;

    public ExternalProfileService(UserService userService) {
        this.userService = userService;
    }

    public Long fetchProfileIdByEmail(String email) {
        return userService.findProfileIdByEmail(email).orElseThrow(ProfileNotFoundException::new);
    }
}
