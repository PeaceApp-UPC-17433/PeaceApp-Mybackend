package com.upc.pre.peaceapp.contacts.domain.model.commands;

public record BlockContactCommand(Long contactId, String requesterEmail) {
}
