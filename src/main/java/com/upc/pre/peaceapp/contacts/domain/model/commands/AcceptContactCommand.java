package com.upc.pre.peaceapp.contacts.domain.model.commands;

public record AcceptContactCommand(Long contactId, String requesterEmail) {
}
