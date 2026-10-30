package com.upc.pre.peaceapp.contacts.interfaces.rest.resources;

import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;

import java.time.Instant;

/** {@code initiatedByMe}: verdadero si quien consulta envió la solicitud original. */
public record ContactResource(Long id, Long profileId, String name, String lastname, String email,
                              ContactStatus status, boolean initiatedByMe, Instant createdAt, Instant respondedAt) {
}
