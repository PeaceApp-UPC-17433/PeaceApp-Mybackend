package com.upc.pre.peaceapp.contacts.application.internal.outboundservices.acl;

import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.services.UserService;
import org.springframework.stereotype.Service;

/** Capa anticorrupción hacia el contexto de usuarios: contactos solo ve el id y el nombre del perfil. */
@Service("contactsExternalProfileService")
public class ExternalProfileService {
    /** Lo único que contactos necesita saber de un perfil. */
    public record ProfileSummary(Long id, String name, String lastname, String email) {
    }

    private final UserService userService;

    public ExternalProfileService(UserService userService) {
        this.userService = userService;
    }

    public Long fetchProfileIdByEmail(String email) {
        return userService.findProfileIdByEmail(email).orElseThrow(ProfileNotFoundException::new);
    }

    public ProfileSummary fetchProfile(Long profileId) {
        var profile = userService.findOptionalById(profileId).orElseThrow(ProfileNotFoundException::new);
        return new ProfileSummary(profile.getId(), profile.getName(), profile.getLastname(), profile.getEmail());
    }
}
