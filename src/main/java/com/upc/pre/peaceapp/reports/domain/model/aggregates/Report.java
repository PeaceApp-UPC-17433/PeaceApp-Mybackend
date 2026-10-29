package com.upc.pre.peaceapp.reports.domain.model.aggregates;

import com.upc.pre.peaceapp.reports.domain.exceptions.InvalidReportStatusTransitionException;
import com.upc.pre.peaceapp.reports.domain.model.events.ReportCreatedEvent;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/**
 * Incidente reportado por un usuario en un lugar concreto. Es la raíz del agregado:
 * todos los cambios pasan por sus métodos para que las reglas se cumplan siempre.
 */
@Getter
@Entity
@Table(name = "reports")
public class Report extends AuditableAbstractAggregateRoot<Report> {
    public static final int TITLE_MAX = 50;
    public static final int DETAIL_MAX = 255;
    public static final int ADDRESS_MAX = 100;
    public static final int IMAGE_MAX = 500;
    /** Tolerancia por desfase de relojes entre el dispositivo y el servidor. */
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    @Column(name = "title", nullable = false, length = TITLE_MAX)
    private String title;

    @Column(name = "detail", length = DETAIL_MAX)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ReportSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Embedded
    private GeoPoint location;

    @Column(name = "address", nullable = false, length = ADDRESS_MAX)
    private String address;

    @Column(name = "image", length = IMAGE_MAX)
    private String image;

    /** Id del perfil (UserProfile) que reportó. */
    @Column(name = "id_user", nullable = false)
    private Long reporterId;

    protected Report() {
    }

    public Report(Long reporterId, String title, String detail, ReportCategory category, ReportSeverity severity,
                  Instant occurredAt, GeoPoint location, String address, String image) {
        if (reporterId == null) throw new IllegalArgumentException("El reporte necesita un usuario");
        this.reporterId = reporterId;
        this.status = ReportStatus.PENDING;
        this.title = requireText(title, "El título", TITLE_MAX);
        this.detail = optionalText(detail, "El detalle", DETAIL_MAX);
        this.category = require(category, "El tipo");
        this.severity = severity == null ? ReportSeverity.MEDIUM : severity;
        this.occurredAt = occurredAt == null ? Instant.now() : requireNotFuture(occurredAt);
        this.location = require(location, "La ubicación");
        this.address = requireText(address, "La dirección", ADDRESS_MAX);
        this.image = optionalText(image, "La imagen", IMAGE_MAX);
    }

    /** Anuncia la creación. Se llama tras el primer guardado, cuando el reporte ya tiene id. */
    public void announceCreation() {
        registerEvent(new ReportCreatedEvent(getId(), reporterId, title, category, severity, location));
    }

    /** Edición parcial: un valor nulo deja el campo como está. */
    public void edit(String title, String detail, ReportCategory category, ReportSeverity severity,
                     Instant occurredAt, GeoPoint location, String address, String image) {
        if (title != null) this.title = requireText(title, "El título", TITLE_MAX);
        if (detail != null) this.detail = optionalText(detail, "El detalle", DETAIL_MAX);
        if (category != null) this.category = category;
        if (severity != null) this.severity = severity;
        if (occurredAt != null) this.occurredAt = requireNotFuture(occurredAt);
        if (location != null) this.location = location;
        if (address != null) this.address = requireText(address, "La dirección", ADDRESS_MAX);
        if (image != null) this.image = optionalText(image, "La imagen", IMAGE_MAX);
    }

    public void changeStatus(ReportStatus next) {
        if (next == null) throw new IllegalArgumentException("El estado es obligatorio");
        if (!status.canTransitionTo(next)) throw new InvalidReportStatusTransitionException(status, next);
        this.status = next;
    }

    public boolean isOwnedBy(Long profileId) {
        return reporterId.equals(profileId);
    }

    private static Instant requireNotFuture(Instant value) {
        if (value.isAfter(Instant.now().plus(FUTURE_TOLERANCE)))
            throw new IllegalArgumentException("La fecha del incidente no puede estar en el futuro");
        return value;
    }

    private static <T> T require(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " es obligatorio");
        return value;
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " es obligatorio");
        return optionalText(value, field, max);
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null) return null;
        var trimmed = value.trim();
        if (trimmed.length() > max)
            throw new IllegalArgumentException(field + " no puede superar " + max + " caracteres");
        return trimmed;
    }
}
