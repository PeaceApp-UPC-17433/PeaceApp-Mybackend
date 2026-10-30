package com.upc.pre.peaceapp.security.iam.domain.model.commands;

import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;

/** Asciende a administrador (ROLE_ADMIN) o degrada a usuario común (ROLE_USER). */
public record ChangeUserRoleCommand(Long userId, Roles role) {
}
