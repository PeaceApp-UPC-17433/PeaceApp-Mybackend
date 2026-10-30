package com.upc.pre.peaceapp.community.domain.model.aggregates;

import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Comentario de un usuario en un reporte. Se retira con borrado lógico: deja de mostrarse pero se conserva
 * para que las denuncias y el historial de moderación sigan teniendo a qué apuntar.
 */
@Getter
@Entity
@Table(name = "report_comments", indexes = @Index(name = "idx_report_comments_report", columnList = "report_id"))
public class ReportComment extends AuditableAbstractAggregateRoot<ReportComment> {
    public static final int TEXT_MAX = 500;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    /** Id del perfil que comentó. */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "content", nullable = false, length = TEXT_MAX)
    private String text;

    @Column(name = "removed", nullable = false)
    private boolean removed;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected ReportComment() {
    }

    public ReportComment(Long reportId, Long authorId, String text) {
        if (reportId == null || authorId == null)
            throw new IllegalArgumentException("El comentario necesita un reporte y un autor");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("El comentario no puede estar vacío");
        var trimmed = text.trim();
        if (trimmed.length() > TEXT_MAX)
            throw new IllegalArgumentException("El comentario no puede superar " + TEXT_MAX + " caracteres");
        this.reportId = reportId;
        this.authorId = authorId;
        this.text = trimmed;
    }

    /** Idempotente: retirar un comentario ya retirado no cambia la fecha. */
    public void remove(Instant now) {
        if (removed) return;
        removed = true;
        removedAt = now;
    }

    public boolean isAuthoredBy(Long profileId) {
        return authorId.equals(profileId);
    }
}
