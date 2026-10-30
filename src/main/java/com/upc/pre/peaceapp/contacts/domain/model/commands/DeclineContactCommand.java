package com.upc.pre.peaceapp.contacts.domain.model.commands;

public record DeclineContactCommand(Long contactId, String requesterEmail) {
}
