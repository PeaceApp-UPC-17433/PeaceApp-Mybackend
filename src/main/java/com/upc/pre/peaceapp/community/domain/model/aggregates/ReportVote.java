package com.upc.pre.peaceapp.community.domain.model.aggregates;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

/** Voto de un usuario sobre un reporte. Hay a lo sumo uno por usuario y reporte; puede cambiar de postura. */
@Getter
@Entity
@Table(name = "report_votes", uniqueConstraints = @UniqueConstraint(columnNames = {"report_id", "voter_id"}))
public class ReportVote extends AuditableAbstractAggregateRoot<ReportVote> {
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    /** Id del perfil que votó. */
    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private VoteType type;

    protected ReportVote() {
    }

    public ReportVote(Long reportId, Long voterId, VoteType type) {
        if (reportId == null || voterId == null)
            throw new IllegalArgumentException("El voto necesita un reporte y un votante");
        if (type == null) throw new IllegalArgumentException("El tipo de voto es obligatorio");
        this.reportId = reportId;
        this.voterId = voterId;
        this.type = type;
    }

    public void changeTo(VoteType type) {
        if (type == null) throw new IllegalArgumentException("El tipo de voto es obligatorio");
        this.type = type;
    }
}
