package com.upc.pre.peaceapp.security.iam.domain.services;

import com.upc.pre.peaceapp.security.iam.domain.model.aggregates.User;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface UserIAMCommandService {
    /** Crea la cuenta y publica {@code UserRegisteredEvent} para que se cree el perfil en la misma transacción. */
    User handle(SignUpCommand command);

    ImmutablePair<User, String> handle(SignInCommand command);

    void handle(ChangePasswordCommand command);

    void handle(ChangeEmailCommand command);

    void handle(RequestPasswordResetCommand command);

    void handle(ResetPasswordCommand command);

    void handle(DeleteAccountCommand command);
}
