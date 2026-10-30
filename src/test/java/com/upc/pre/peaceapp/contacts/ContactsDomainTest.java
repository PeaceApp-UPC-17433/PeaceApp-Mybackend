package com.upc.pre.peaceapp.contacts;

import com.upc.pre.peaceapp.contacts.domain.exceptions.ContactConflictException;
import com.upc.pre.peaceapp.contacts.domain.model.aggregates.TrustedContact;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactsDomainTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void requestStartsPendingAndPairKeyIgnoresDirection() {
        var contact = new TrustedContact(7L, 3L);
        assertThat(contact.getStatus()).isEqualTo(ContactStatus.PENDING);
        assertThat(contact.getPairKey()).isEqualTo("3:7").isEqualTo(TrustedContact.pairKey(3L, 7L));
        assertThat(contact.otherParty(7L)).isEqualTo(3L);
        assertThat(contact.involves(5L)).isFalse();
    }

    @Test
    void cannotAddYourself() {
        assertThatThrownBy(() -> new TrustedContact(1L, 1L)).hasMessage("No puedes agregarte a ti mismo");
        assertThatThrownBy(() -> new TrustedContact(null, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyTheAddresseeAcceptsAndOnlyOnce() {
        var contact = new TrustedContact(1L, 2L);
        assertThatThrownBy(() -> contact.accept(1L, NOW)).isInstanceOf(ForbiddenOperationException.class);
        contact.accept(2L, NOW);
        assertThat(contact.isAccepted()).isTrue();
        assertThat(contact.getRespondedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> contact.accept(2L, NOW)).isInstanceOf(ContactConflictException.class);
        assertThatThrownBy(() -> contact.assertCanDecline(2L)).isInstanceOf(ContactConflictException.class);
    }

    @Test
    void blockingRecordsWhoBlockedAndOnlyThatPersonCanRemoveIt() {
        var contact = new TrustedContact(1L, 2L);
        contact.block(2L, NOW);
        assertThat(contact.getStatus()).isEqualTo(ContactStatus.BLOCKED);
        assertThat(contact.getBlockedBy()).isEqualTo(2L);
        assertThatThrownBy(() -> contact.block(1L, NOW)).isInstanceOf(ContactConflictException.class);
        assertThatThrownBy(() -> contact.assertCanRemove(1L)).isInstanceOf(ForbiddenOperationException.class);
        contact.assertCanRemove(2L);
        assertThatThrownBy(() -> contact.block(9L, NOW)).isInstanceOf(ForbiddenOperationException.class);
    }
}
