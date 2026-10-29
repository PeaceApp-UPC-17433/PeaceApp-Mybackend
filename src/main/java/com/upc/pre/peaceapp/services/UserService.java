package com.upc.pre.peaceapp.services;

import com.upc.pre.peaceapp.models.UserProfile;
import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.repositories.UserRepository;
import com.upc.pre.peaceapp.schemas.UpdateUserProfileSchema;
import com.upc.pre.peaceapp.security.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.ChangeEmailCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.DeleteAccountCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.events.UserRegisteredEvent;
import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.EmailAddress;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMCommandService;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Casos de uso del perfil. Un perfil solo lo gestiona su dueño (el correo del token
 * coincide con el del perfil) o un administrador.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final UserIAMCommandService userIAMCommandService;

    public UserService(UserRepository repository, UserIAMCommandService userIAMCommandService) {
        this.repository = repository;
        this.userIAMCommandService = userIAMCommandService;
    }

    /** Reacción a {@link UserRegisteredEvent}: corre dentro de la transacción del registro. */
    public UserProfile createProfile(UserRegisteredEvent event) {
        return repository.save(new UserProfile(event.name().trim(), event.email(), event.lastname().trim(),
                event.phonenumber(), event.userId()));
    }

    @Transactional(readOnly = true)
    public UserProfile findByEmail(String email, String requesterEmail, boolean admin) {
        var profile = repository.findByEmail(EmailAddress.of(email).value()).orElseThrow(ProfileNotFoundException::new);
        assertCanManage(profile, requesterEmail, admin);
        return profile;
    }

    public UserProfile update(Long id, UpdateUserProfileSchema changes, String requesterEmail, boolean admin) {
        var profile = findOwned(id, requesterEmail, admin);

        if (changes.email() != null) {
            var newEmail = EmailAddress.of(changes.email()).value();
            if (!newEmail.equals(profile.getEmail())) {
                if (repository.existsByEmail(newEmail)) throw new EmailAlreadyRegisteredException(newEmail);
                userIAMCommandService.handle(new ChangeEmailCommand(profile.getEmail(), newEmail));
                profile.setEmail(newEmail);
            }
        }
        if (changes.name() != null) profile.setName(changes.name().trim());
        if (changes.lastname() != null) profile.setLastname(changes.lastname().trim());
        if (changes.phonenumber() != null) profile.setPhonenumber(changes.phonenumber());
        if (changes.profile_image() != null) profile.setProfile_image(changes.profile_image());
        return repository.save(profile);
    }

    /** Elimina perfil y cuenta IAM de forma atómica. */
    public void delete(Long id, String requesterEmail, boolean admin) {
        var profile = findOwned(id, requesterEmail, admin);
        userIAMCommandService.handle(new DeleteAccountCommand(profile.getEmail()));
        repository.delete(profile);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findOptionalById(Long id) {
        return repository.findById(id);
    }

    private UserProfile findOwned(Long id, String requesterEmail, boolean admin) {
        var profile = repository.findById(id).orElseThrow(ProfileNotFoundException::new);
        assertCanManage(profile, requesterEmail, admin);
        return profile;
    }

    private void assertCanManage(UserProfile profile, String requesterEmail, boolean admin) {
        if (!admin && !profile.getEmail().equals(requesterEmail))
            throw new ForbiddenOperationException("No tienes permiso para acceder a este perfil");
    }
}
