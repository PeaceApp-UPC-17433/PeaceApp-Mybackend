package com.upc.pre.peaceapp.security.iam.application.internal.commandservices;

import com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.hashing.HashingService;
import com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.notifications.PasswordResetNotifier;
import com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.tokens.TokenService;
import com.upc.pre.peaceapp.security.iam.domain.exceptions.*;
import com.upc.pre.peaceapp.security.iam.domain.model.aggregates.User;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.*;
import com.upc.pre.peaceapp.security.iam.domain.model.entities.PasswordResetToken;
import com.upc.pre.peaceapp.security.iam.domain.model.events.UserRegisteredEvent;
import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.EmailAddress;
import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMCommandService;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenRepository;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class UserIAMCommandServiceImpl implements UserIAMCommandService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserIAMRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final PasswordResetNotifier passwordResetNotifier;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration resetTokenTtl;

    public UserIAMCommandServiceImpl(UserIAMRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordResetTokenRepository resetTokenRepository,
                                     HashingService hashingService,
                                     TokenService tokenService,
                                     PasswordResetNotifier passwordResetNotifier,
                                     ApplicationEventPublisher eventPublisher,
                                     @Value("${authorization.password-reset.expiration-minutes:30}") long resetTokenMinutes) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.passwordResetNotifier = passwordResetNotifier;
        this.eventPublisher = eventPublisher;
        this.resetTokenTtl = Duration.ofMinutes(resetTokenMinutes);
    }

    @Override
    public User handle(SignUpCommand command) {
        var email = EmailAddress.of(command.email());
        if (userRepository.existsByUsername(email.value()))
            throw new EmailAlreadyRegisteredException(email.value());

        // El cliente nunca elige roles: toda cuenta nueva es ROLE_USER.
        var defaultRole = roleRepository.findByName(Roles.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER no está sembrado"));
        var user = userRepository.save(new User(email.value(), hashingService.encode(command.password()), List.of(defaultRole)));

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), email.value(), command.name(), command.lastname(), command.phonenumber()));
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public ImmutablePair<User, String> handle(SignInCommand command) {
        var user = userRepository.findByUsername(EmailAddress.of(command.email()).value())
                .filter(u -> hashingService.matches(command.password(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);
        return ImmutablePair.of(user, tokenService.generateToken(user.getUsername()));
    }

    @Override
    public void handle(ChangePasswordCommand command) {
        var user = findByEmail(command.email());
        if (!hashingService.matches(command.currentPassword(), user.getPassword()))
            throw new InvalidCurrentPasswordException();
        user.changePassword(hashingService.encode(command.newPassword()));
        resetTokenRepository.deleteByUserId(user.getId());
    }

    @Override
    public void handle(ChangeEmailCommand command) {
        var newEmail = EmailAddress.of(command.newEmail());
        var user = findByEmail(command.currentEmail());
        if (user.getUsername().equals(newEmail.value())) return;
        if (userRepository.existsByUsername(newEmail.value()))
            throw new EmailAlreadyRegisteredException(newEmail.value());
        user.changeUsername(newEmail.value());
    }

    @Override
    public void handle(RequestPasswordResetCommand command) {
        // Si el correo no existe no se revela: el controlador responde igual en ambos casos.
        userRepository.findByUsername(EmailAddress.of(command.email()).value()).ifPresent(user -> {
            resetTokenRepository.deleteByUserId(user.getId());
            var rawToken = generateRawToken();
            resetTokenRepository.save(new PasswordResetToken(user.getId(), rawToken, Instant.now().plus(resetTokenTtl)));
            passwordResetNotifier.sendResetToken(user.getUsername(), rawToken);
        });
    }

    @Override
    public void handle(ResetPasswordCommand command) {
        var token = resetTokenRepository.findByTokenHash(PasswordResetToken.hash(command.token()))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (token.isExpired(Instant.now())) {
            resetTokenRepository.delete(token);
            throw new InvalidPasswordResetTokenException();
        }
        var user = userRepository.findById(token.getUserId()).orElseThrow(InvalidPasswordResetTokenException::new);
        user.changePassword(hashingService.encode(command.newPassword()));
        resetTokenRepository.delete(token); // un solo uso
    }

    @Override
    public void handle(DeleteAccountCommand command) {
        var user = findByEmail(command.email());
        resetTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user); // la tabla user_roles se limpia por la relación ManyToMany
    }

    @Override
    public boolean handle(SeedAdminCommand command) {
        var email = EmailAddress.of(command.email());
        if (userRepository.existsByUsername(email.value())) return false;
        var roles = List.of(
                roleRepository.findByName(Roles.ROLE_ADMIN).orElseThrow(() -> new IllegalStateException("ROLE_ADMIN no está sembrado")),
                roleRepository.findByName(Roles.ROLE_USER).orElseThrow(() -> new IllegalStateException("ROLE_USER no está sembrado")));
        var user = userRepository.save(new User(email.value(), hashingService.encode(command.password()), roles));
        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), email.value(), command.name(), command.lastname(), null));
        return true;
    }

    private User findByEmail(String email) {
        return userRepository.findByUsername(EmailAddress.of(email).value())
                .orElseThrow(UserNotFoundException::new);
    }

    private static String generateRawToken() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
