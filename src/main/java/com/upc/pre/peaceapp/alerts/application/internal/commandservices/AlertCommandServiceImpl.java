package com.upc.pre.peaceapp.alerts.application.internal.commandservices;

import com.upc.pre.peaceapp.alerts.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.alerts.domain.exceptions.AlertNotFoundException;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.domain.model.commands.*;
import com.upc.pre.peaceapp.alerts.domain.model.events.AlertGeneratedEvent;
import com.upc.pre.peaceapp.alerts.domain.services.AlertCommandService;
import com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories.AlertSettingsRepository;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AlertCommandServiceImpl implements AlertCommandService {
    private static final double METERS_PER_DEGREE = 111_000;

    private final AlertRepository alertRepository;
    private final AlertSettingsRepository settingsRepository;
    private final ExternalProfileService externalProfileService;
    private final ApplicationEventPublisher eventPublisher;

    public AlertCommandServiceImpl(AlertRepository alertRepository, AlertSettingsRepository settingsRepository,
                                   ExternalProfileService externalProfileService,
                                   ApplicationEventPublisher eventPublisher) {
        this.alertRepository = alertRepository;
        this.settingsRepository = settingsRepository;
        this.externalProfileService = externalProfileService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Corre en su propia transacción porque se dispara después de confirmar la creación del reporte:
     * si falla, el reporte ya está guardado y no debe verse afectado.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int handle(GenerateProximityAlertsCommand command) {
        var origin = command.location();
        var now = Instant.now();
        var latDelta = AlertSettings.MAX_RADIUS_METERS / METERS_PER_DEGREE;
        // Cerca de los polos un grado de longitud mide casi nada; el mínimo evita un rectángulo infinito.
        var lngDelta = latDelta / Math.max(Math.cos(Math.toRadians(origin.latitude())), 0.01);

        var created = 0;
        for (var settings : settingsRepository.findNear(origin.latitude() - latDelta, origin.latitude() + latDelta,
                origin.longitude() - lngDelta, origin.longitude() + lngDelta,
                now.minus(AlertSettings.LOCATION_FRESHNESS))) {
            if (settings.getProfileId().equals(command.reporterId())) continue;
            if (!settings.wantsAlertFor(command.category(), command.severity(), now)) continue;
            var distance = (int) Math.round(settings.getLastLocation().distanceMetersTo(origin));
            if (distance > settings.getRadiusMeters()) continue;
            if (alertRepository.existsByRecipientIdAndReportId(settings.getProfileId(), command.reportId())) continue;

            var alert = alertRepository.save(new Alert(settings.getProfileId(), command.reportId(), command.title(),
                    command.category(), command.severity(), origin, distance));
            eventPublisher.publishEvent(new AlertGeneratedEvent(alert.getId(), alert.getRecipientId(),
                    alert.getReportId(), alert.getReportTitle(), alert.getReportType(), alert.getReportSeverity(),
                    alert.getDistanceMeters()));
            created++;
        }
        return created;
    }

    @Override
    public Alert handle(MarkAlertReadCommand command) {
        var alert = findOwned(command.alertId(), command.requesterEmail());
        alert.markRead(Instant.now());
        return alertRepository.save(alert);
    }

    @Override
    public int handle(MarkAllAlertsReadCommand command) {
        return alertRepository.markAllRead(requesterId(command.requesterEmail()), Instant.now());
    }

    @Override
    public void handle(DeleteAlertCommand command) {
        alertRepository.delete(findOwned(command.alertId(), command.requesterEmail()));
    }

    @Override
    public void handle(DeleteMyAlertsCommand command) {
        alertRepository.deleteAllByRecipient(requesterId(command.requesterEmail()));
    }

    @Override
    public AlertSettings handle(UpdateAlertSettingsCommand command) {
        var settings = settingsOf(requesterId(command.requesterEmail()));
        settings.configure(command.radiusMeters(), command.minSeverity(), command.ignoredTypes(), command.mutedUntil());
        return settingsRepository.save(settings);
    }

    @Override
    public AlertSettings handle(UpdateLastLocationCommand command) {
        var settings = settingsOf(requesterId(command.requesterEmail()));
        settings.moveTo(command.location(), Instant.now());
        return settingsRepository.save(settings);
    }

    private AlertSettings settingsOf(Long profileId) {
        return settingsRepository.findByProfileId(profileId).orElseGet(() -> AlertSettings.defaultsFor(profileId));
    }

    private Long requesterId(String email) {
        return externalProfileService.fetchProfileIdByEmail(email);
    }

    private Alert findOwned(Long alertId, String requesterEmail) {
        var alert = alertRepository.findById(alertId).orElseThrow(AlertNotFoundException::new);
        if (!alert.isOwnedBy(requesterId(requesterEmail)))
            throw new ForbiddenOperationException("No tienes permiso para modificar esta alerta");
        return alert;
    }
}
