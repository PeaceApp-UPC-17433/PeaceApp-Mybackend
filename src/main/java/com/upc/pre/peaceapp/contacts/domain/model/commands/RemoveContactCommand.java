package com.upc.pre.peaceapp.contacts.domain.model.commands;

public record RemoveContactCommand(Long contactId, String requesterEmail) {
}
