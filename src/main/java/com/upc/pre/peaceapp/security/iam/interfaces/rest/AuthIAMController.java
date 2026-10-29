package com.upc.pre.peaceapp.security.iam.interfaces.rest;

import com.upc.pre.peaceapp.security.iam.domain.model.commands.RequestPasswordResetCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.ResetPasswordCommand;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMCommandService;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * AuthenticationController
 * <p>
 * Endpoints públicos: sign-up, sign-in, forgot-password y reset-password.
 * Endpoint autenticado: change-password.
 * Los errores los traduce {@code GlobalExceptionHandler}.
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication Endpoints")
public class AuthIAMController {
    private final UserIAMCommandService userCommandService;

    public AuthIAMController(UserIAMCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    /** Registro unificado: crea la identidad y el perfil en una sola transacción. */
    @PostMapping("/sign-up")
    public ResponseEntity<UserIAMResource> signUp(@Valid @RequestBody SignUpResource signUpResource) {
        var user = userCommandService.handle(SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource));
        return new ResponseEntity<>(UserResourceFromEntityAssembler.toResourceFromEntity(user), HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource signInResource) {
        var result = userCommandService.handle(SignInCommandFromResourceAssembler.toCommandFromResource(signInResource));
        return ResponseEntity.ok(AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(result.getLeft(), result.getRight()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordResource resource) {
        userCommandService.handle(ChangePasswordCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource));
        return ResponseEntity.noContent().build();
    }

    /** Responde siempre 202, exista o no el correo, para no revelar qué cuentas existen. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordResource resource) {
        userCommandService.handle(new RequestPasswordResetCommand(resource.email()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordResource resource) {
        userCommandService.handle(new ResetPasswordCommand(resource.token(), resource.newPassword()));
        return ResponseEntity.noContent().build();
    }
}
