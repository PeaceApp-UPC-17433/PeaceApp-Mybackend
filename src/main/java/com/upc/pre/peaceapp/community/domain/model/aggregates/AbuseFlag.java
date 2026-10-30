package com.upc.pre.peaceapp.community.domain.model.aggregates;

import com.upc.pre.peaceapp.community.domain.exceptions.AbuseFlagAlreadyResolvedException;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagReason;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/** Denuncia de un usuario contra un reporte o un comentario. Un moderador la resuelve una sola vez. */
@Getter
@Entity
@Table(name = "abuse_flags", uniqueConstraints = @UniqueConstraint(columnNames = {"target_type", "target_id", "reporter_id"}))
public class AbuseFlag extends AuditableAbstractAggregateRoot<AbuseFlag> {
    public static final int DETAILS_MAX = 255;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private FlagTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** Id del perfil que denuncia. */
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private FlagReason reason;

    @Column(name = "details", length = DETAILS_MAX)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FlagStatus status;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = DETAILS_MAX)
    private String resolutionNote;

    protected AbuseFlag() {
    }

    public AbuseFlag(FlagTargetType targetType, Long targetId, Long reporterId, FlagReason reason, String details) {
        if (targetType == null || targetId == null) throw new IllegalArgumentException("Falta el contenido denunciado");
        if (reporterId == null) throw new IllegalArgumentException("La denuncia necesita un usuario");
        if (reason == null) throw new IllegalArgumentException("El motivo es obligatorio");
        this.targetType = targetType;
        this.targetId = targetId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.details = optionalText(details, "El detalle");
        this.status = FlagStatus.OPEN;
    }

    public void resolve(FlagStatus resolution, Long moderatorId, String note, Instant now) {
        if (resolution == null || !resolution.isResolution())
            throw new IllegalArgumentException("La resolución debe ser DISMISSED o ACTION_TAKEN");
        if (status != FlagStatus.OPEN) throw new AbuseFlagAlreadyResolvedException();
        this.status = resolution;
        this.resolvedBy = moderatorId;
        this.resolvedAt = now;
        this.resolutionNote = optionalText(note, "La nota");
    }

    public boolean isOpen() {
        return status == FlagStatus.OPEN;
    }

    private static String optionalText(String value, String field) {
        if (value == null || value.isBlank()) return null;
        var trimmed = value.trim();
        if (trimmed.length() > DETAILS_MAX)
            throw new IllegalArgumentException(field + " no puede superar " + DETAILS_MAX + " caracteres");
        return trimmed;
    }
}
