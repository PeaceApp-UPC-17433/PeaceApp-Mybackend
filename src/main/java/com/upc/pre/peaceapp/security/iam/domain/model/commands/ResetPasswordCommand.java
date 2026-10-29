package com.upc.pre.peaceapp.security.iam.domain.model.commands;

public record ResetPasswordCommand(String token, String newPassword) {
}
