package com.upc.pre.peaceapp.security.iam.interfaces.rest.transform;

import com.upc.pre.peaceapp.security.iam.domain.model.commands.ChangePasswordCommand;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.ChangePasswordResource;

public class ChangePasswordCommandFromResourceAssembler {
    /** El correo sale del token autenticado, nunca del cuerpo de la petición. */
    public static ChangePasswordCommand toCommandFromResource(String authenticatedEmail, ChangePasswordResource resource) {
        return new ChangePasswordCommand(authenticatedEmail, resource.currentPassword(), resource.newPassword());
    }
}
