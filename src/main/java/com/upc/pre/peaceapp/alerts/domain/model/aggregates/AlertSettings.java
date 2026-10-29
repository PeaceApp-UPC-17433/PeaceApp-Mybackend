package com.upc.pre.peaceapp.alerts.domain.model.aggregates;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Preferencias de alerta de un usuario y su última ubicación conocida. Con estos datos el sistema
 * decide si un reporte nuevo le interesa. Se crea la primera vez que el usuario envía su ubicación
 * o guarda su configuración; mientras no exista se usan los valores por defecto.
 */
@Getter
@Entity
@Table(name = "alert_settings", uniqueConstraints = @UniqueConstraint(columnNames = "profile_id"))
public class AlertSettings extends AuditableAbstractAggregateRoot<AlertSettings> {
    public static final int DEFAULT_RADIUS_METERS = 1000;
    public static final int MIN_RADIUS_METERS = 100;
    public static final int MAX_RADIUS_METERS = 20_000;
    /** Una ubicación más vieja que esto ya no sirve para avisar de lo que pasa cerca. */
    public static final Duration LOCATION_FRESHNESS = Duration.ofHours(24);

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, length = 20)
    private ReportSeverity minSeverity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_settings_ignored_types", joinColumns = @JoinColumn(name = "settings_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private Set<ReportCategory> ignoredTypes = new HashSet<>();

    /** Hasta cuándo están silenciadas las alertas; nulo si no lo están. */
    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "last_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "last_longitude"))})
    private GeoPoint lastLocation;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    protected AlertSettings() {
    }

    public static AlertSettings defaultsFor(Long profileId) {
        if (profileId == null) throw new IllegalArgumentException("La configuración necesita un usuario");
        var settings = new AlertSettings();
        settings.profileId = profileId;
        settings.radiusMeters = DEFAULT_RADIUS_METERS;
        settings.minSeverity = ReportSeverity.LOW;
        return settings;
    }

    /** Reemplaza la configuración completa; un conjunto o fecha nulos significan "ninguno". */
    public void configure(int radiusMeters, ReportSeverity minSeverity, Set<ReportCategory> ignoredTypes,
                          Instant mutedUntil) {
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS)
            throw new IllegalArgumentException("El radio debe estar entre " + MIN_RADIUS_METERS + " y "
                    + MAX_RADIUS_METERS + " metros");
        if (minSeverity == null) throw new IllegalArgumentException("La severidad mínima es obligatoria");
        this.radiusMeters = radiusMeters;
        this.minSeverity = minSeverity;
        this.ignoredTypes = ignoredTypes == null ? new HashSet<>() : new HashSet<>(ignoredTypes);
        this.mutedUntil = mutedUntil;
    }

    public void moveTo(GeoPoint location, Instant now) {
        if (location == null) throw new IllegalArgumentException("La ubicación es obligatoria");
        this.lastLocation = location;
        this.locationUpdatedAt = now;
    }

    public boolean isMuted(Instant now) {
        return mutedUntil != null && mutedUntil.isAfter(now);
    }

    public boolean hasFreshLocation(Instant now) {
        return lastLocation != null && locationUpdatedAt != null
                && locationUpdatedAt.isAfter(now.minus(LOCATION_FRESHNESS));
    }

    /** Si al usuario le interesa un incidente de este tipo y severidad, sin considerar la distancia. */
    public boolean wantsAlertFor(ReportCategory type, ReportSeverity severity, Instant now) {
        return !isMuted(now) && !ignoredTypes.contains(type) && severity.compareTo(minSeverity) >= 0;
    }
}
