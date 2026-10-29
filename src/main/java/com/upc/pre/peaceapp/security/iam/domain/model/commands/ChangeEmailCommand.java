package com.upc.pre.peaceapp.security.iam.domain.model.commands;

public record ChangeEmailCommand(String currentEmail, String newEmail) {
}
