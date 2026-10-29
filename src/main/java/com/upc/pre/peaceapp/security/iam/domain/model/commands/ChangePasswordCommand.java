package com.upc.pre.peaceapp.security.iam.domain.model.commands;

public record ChangePasswordCommand(String email, String currentPassword, String newPassword) {
}
