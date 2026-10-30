package com.upc.pre.peaceapp.contacts.domain.model.aggregates;

import com.upc.pre.peaceapp.contacts.domain.exceptions.ContactConflictException;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Relación de confianza entre dos perfiles. Hay una sola por pareja, sin importar quién la inició
 * ({@code pairKey} la hace única en la base). Nace pendiente, el destinatario la acepta y cualquiera de los dos
 * puede bloquearla.
 */
@Getter
@Entity
@Table(name = "trusted_contacts", uniqueConstraints = @UniqueConstraint(columnNames = "pair_key"))
public class TrustedContact extends AuditableAbstractAggregateRoot<TrustedContact> {
    /** Perfil que envió la solicitud. */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    /** Perfil que recibe la solicitud. */
    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    /** Los dos ids ordenados ("menor:mayor"): impide parejas repetidas aunque se cruce una solicitud en sentido contrario. */
    @Column(name = "pair_key", nullable = false, length = 41)
    private String pairKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status;

    /** Quien bloqueó; solo esa persona puede deshacer el bloqueo. */
    @Column(name = "blocked_by")
    private Long blockedBy;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TrustedContact() {
    }

    public TrustedContact(Long requesterId, Long addresseeId) {
        if (requesterId == null || addresseeId == null)
            throw new IllegalArgumentException("La solicitud necesita dos usuarios");
        if (requesterId.equals(addresseeId)) throw new IllegalArgumentException("No puedes agregarte a ti mismo");
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.pairKey = pairKey(requesterId, addresseeId);
        this.status = ContactStatus.PENDING;
    }

    public static String pairKey(Long a, Long b) {
        return Math.min(a, b) + ":" + Math.max(a, b);
    }

    public boolean involves(Long profileId) {
        return requesterId.equals(profileId) || addresseeId.equals(profileId);
    }

    public Long otherParty(Long profileId) {
        return requesterId.equals(profileId) ? addresseeId : requesterId;
    }

    public boolean isAccepted() {
        return status == ContactStatus.ACCEPTED;
    }

    /** Solo el destinatario acepta, y solo una solicitud pendiente. */
    public void accept(Long profileId, Instant now) {
        requireAddressee(profileId);
        if (status != ContactStatus.PENDING) throw new ContactConflictException("La solicitud ya no está pendiente");
        this.status = ContactStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** Solo el destinatario puede rechazar una solicitud pendiente (el servicio la elimina). */
    public void assertCanDecline(Long profileId) {
        requireAddressee(profileId);
        if (status != ContactStatus.PENDING) throw new ContactConflictException("La solicitud ya no está pendiente");
    }

    public void block(Long profileId, Instant now) {
        requireParty(profileId);
        if (status == ContactStatus.BLOCKED) throw new ContactConflictException("El contacto ya está bloqueado");
        this.status = ContactStatus.BLOCKED;
        this.blockedBy = profileId;
        this.respondedAt = now;
    }

    /** Cualquiera puede retirar la relación, salvo un bloqueo, que solo deshace quien lo hizo. */
    public void assertCanRemove(Long profileId) {
        requireParty(profileId);
        if (status == ContactStatus.BLOCKED && !profileId.equals(blockedBy))
            throw new ForbiddenOperationException("Solo quien bloqueó puede quitar el bloqueo");
    }

    private void requireAddressee(Long profileId) {
        if (!addresseeId.equals(profileId))
            throw new ForbiddenOperationException("Solo quien recibe la solicitud puede responderla");
    }

    private void requireParty(Long profileId) {
        if (!involves(profileId)) throw new ForbiddenOperationException("No participas en esta relación");
    }
}
