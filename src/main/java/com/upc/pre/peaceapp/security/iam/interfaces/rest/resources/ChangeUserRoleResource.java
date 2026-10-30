package com.upc.pre.peaceapp.security.iam.interfaces.rest.resources;

import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleResource(@NotNull(message = "El rol es obligatorio") Roles role) {
}
