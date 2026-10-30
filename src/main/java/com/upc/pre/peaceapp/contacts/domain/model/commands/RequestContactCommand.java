package com.upc.pre.peaceapp.contacts.domain.model.commands;

public record RequestContactCommand(String requesterEmail, String targetEmail) {
}
