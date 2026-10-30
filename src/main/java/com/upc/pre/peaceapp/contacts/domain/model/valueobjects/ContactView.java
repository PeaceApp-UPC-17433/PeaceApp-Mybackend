package com.upc.pre.peaceapp.contacts.domain.model.valueobjects;

import java.time.Instant;

/** Una relación vista desde un usuario: quién es el otro y quién la inició. */
public record ContactView(Long id, Long profileId, String name, String lastname, String email, ContactStatus status,
                          boolean initiatedByMe, Instant createdAt, Instant respondedAt) {
}
