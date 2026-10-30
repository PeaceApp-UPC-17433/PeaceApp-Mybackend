package com.upc.pre.peaceapp.community.domain.model.aggregates;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.ModerationActionType;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

/** Registro inmutable de algo que hizo un moderador: queda en el historial para poder auditarlo. */
@Getter
@Entity
@Table(name = "moderation_actions")
public class ModerationAction extends AuditableAbstractAggregateRoot<ModerationAction> {
    public static final int NOTE_MAX = 255;

    /** Id del perfil del moderador. */
    @Column(name = "moderator_id", nullable = false)
    private Long moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ModerationActionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private FlagTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "note", length = NOTE_MAX)
    private String note;

    protected ModerationAction() {
    }

    public ModerationAction(Long moderatorId, ModerationActionType type, FlagTargetType targetType, Long targetId,
                            String note) {
        if (moderatorId == null || type == null || targetType == null || targetId == null)
            throw new IllegalArgumentException("La acción de moderación está incompleta");
        this.moderatorId = moderatorId;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
}
