package com.upc.pre.peaceapp.security.iam.interfaces.rest.transform;


import com.upc.pre.peaceapp.security.iam.domain.model.commands.SignUpCommand;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(resource.email(), resource.password(), resource.name(), resource.lastname(), resource.phonenumber());
    }

}
