package com.upc.pre.peaceapp.security.iam.domain.model.commands;

public record SignUpCommand(String email, String password, String name, String lastname, String phonenumber) {
}
