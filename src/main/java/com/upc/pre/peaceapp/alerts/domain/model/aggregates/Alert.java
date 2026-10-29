package com.upc.pre.peaceapp.alerts.domain.model.aggregates;

import com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Aviso para un usuario sobre un reporte cercano. Guarda una copia de los datos del reporte
 * para que el historial siga siendo legible aunque el reporte se edite o se elimine.
 * Hay a lo sumo una alerta por usuario y reporte.
 */
@Getter
@Entity
@Table(name = "user_alerts", uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_id", "report_id"}))
public class Alert extends AuditableAbstractAggregateRoot<Alert> {
    public static final int TITLE_MAX = 50;

    /** Id del perfil que recibe la alerta. */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "report_title", nullable = false, length = TITLE_MAX)
    private String reportTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportCategory reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_severity", nullable = false, length = 20)
    private ReportSeverity reportSeverity;

    @Embedded
    private GeoPoint reportLocation;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "read_at")
    private Instant readAt;

    protected Alert() {
    }

    public Alert(Long recipientId, Long reportId, String reportTitle, ReportCategory reportType,
                 ReportSeverity reportSeverity, GeoPoint reportLocation, int distanceMeters) {
        if (recipientId == null || reportId == null)
            throw new IllegalArgumentException("La alerta necesita un usuario y un reporte");
        if (reportLocation == null) throw new IllegalArgumentException("La alerta necesita la ubicación del reporte");
        if (distanceMeters < 0) throw new IllegalArgumentException("La distancia no puede ser negativa");
        this.recipientId = recipientId;
        this.reportId = reportId;
        this.reportTitle = reportTitle.length() > TITLE_MAX ? reportTitle.substring(0, TITLE_MAX) : reportTitle;
        this.reportType = reportType;
        this.reportSeverity = reportSeverity;
        this.reportLocation = reportLocation;
        this.distanceMeters = distanceMeters;
        this.status = AlertStatus.UNREAD;
    }

    /** Idempotente: marcar como leída una alerta ya leída no cambia la fecha de lectura. */
    public void markRead(Instant now) {
        if (status == AlertStatus.READ) return;
        status = AlertStatus.READ;
        readAt = now;
    }

    public boolean isOwnedBy(Long profileId) {
        return recipientId.equals(profileId);
    }
}
