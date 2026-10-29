package com.upc.pre.peaceapp.notifications.domain.model.aggregates;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/** Aviso dentro de la app. Es el historial de lo que se le notificó al usuario. */
@Getter
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_id", "kind", "reference_id"}))
public class Notification extends AuditableAbstractAggregateRoot<Notification> {
    public static final int TITLE_MAX = 100;
    public static final int BODY_MAX = 255;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private NotificationKind kind;

    @Column(name = "title", nullable = false, length = TITLE_MAX)
    private String title;

    @Column(name = "body", nullable = false, length = BODY_MAX)
    private String body;

    /** Id del hecho que originó el aviso (la alerta), para no notificar dos veces lo mismo. */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(Long recipientId, NotificationKind kind, String title, String body, Long referenceId) {
        if (recipientId == null || kind == null || referenceId == null)
            throw new IllegalArgumentException("La notificación necesita destinatario, tipo y referencia");
        this.recipientId = recipientId;
        this.kind = kind;
        this.title = truncate(title, TITLE_MAX);
        this.body = truncate(body, BODY_MAX);
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(Instant now) {
        if (readAt == null) readAt = now;
    }

    public boolean isOwnedBy(Long profileId) {
        return recipientId.equals(profileId);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("El texto de la notificación es obligatorio");
        return value.length() > max ? value.substring(0, max) : value;
    }
}
