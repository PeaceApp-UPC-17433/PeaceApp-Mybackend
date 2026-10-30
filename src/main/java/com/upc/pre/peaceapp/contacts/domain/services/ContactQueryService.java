package com.upc.pre.peaceapp.contacts.domain.services;

import com.upc.pre.peaceapp.contacts.domain.model.queries.*;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactView;

import java.util.List;

public interface ContactQueryService {
    List<ContactView> handle(GetContactsQuery query);

    List<ContactView> handle(GetIncomingRequestsQuery query);

    List<ContactView> handle(GetOutgoingRequestsQuery query);

    List<ContactView> handle(GetBlockedContactsQuery query);

    /** Para otros contextos (compartir ubicación, emergencias): ids de perfil de los contactos aceptados. */
    List<Long> handle(GetAcceptedContactProfileIdsQuery query);
}
