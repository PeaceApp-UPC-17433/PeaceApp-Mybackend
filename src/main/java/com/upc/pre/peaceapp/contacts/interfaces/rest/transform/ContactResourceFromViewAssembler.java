package com.upc.pre.peaceapp.contacts.interfaces.rest.transform;

import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactView;
import com.upc.pre.peaceapp.contacts.interfaces.rest.resources.ContactResource;

public final class ContactResourceFromViewAssembler {
    private ContactResourceFromViewAssembler() {
    }

    public static ContactResource toResource(ContactView view) {
        return new ContactResource(view.id(), view.profileId(), view.name(), view.lastname(), view.email(),
                view.status(), view.initiatedByMe(), view.createdAt(), view.respondedAt());
    }
}
