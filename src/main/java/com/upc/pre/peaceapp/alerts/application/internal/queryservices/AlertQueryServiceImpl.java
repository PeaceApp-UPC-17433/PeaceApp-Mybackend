package com.upc.pre.peaceapp.alerts.application.internal.queryservices;

import com.upc.pre.peaceapp.alerts.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.alerts.domain.exceptions.AlertNotFoundException;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.domain.model.queries.*;
import com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus;
import com.upc.pre.peaceapp.alerts.domain.services.AlertQueryService;
import com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories.AlertSettingsRepository;
import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertQueryServiceImpl implements AlertQueryService {
    private final AlertRepository alertRepository;
    private final AlertSettingsRepository settingsRepository;
    private final ExternalProfileService externalProfileService;

    public AlertQueryServiceImpl(AlertRepository alertRepository, AlertSettingsRepository settingsRepository,
                                 ExternalProfileService externalProfileService) {
        this.alertRepository = alertRepository;
        this.settingsRepository = settingsRepository;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public Page<Alert> handle(GetAlertsQuery query) {
        var recipientId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return query.unreadOnly()
                ? alertRepository.findByRecipientIdAndStatus(recipientId, AlertStatus.UNREAD, query.pageable())
                : alertRepository.findByRecipientId(recipientId, query.pageable());
    }

    /** Alertas de un perfil concreto: solo el dueño o un admin; distingue "sin alertas" de "perfil inexistente". */
    @Override
    public Page<Alert> handle(GetAlertsByUserQuery query) {
        if (!query.admin()) {
            var requesterId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
            if (!requesterId.equals(query.userId()))
                throw new ForbiddenOperationException("No tienes permiso para ver las alertas de otro usuario");
        } else if (!externalProfileService.profileExists(query.userId())) {
            throw new ProfileNotFoundException();
        }
        return alertRepository.findByRecipientId(query.userId(), query.pageable());
    }

    @Override
    public Alert handle(GetAlertByIdQuery query) {
        var alert = alertRepository.findById(query.alertId()).orElseThrow(AlertNotFoundException::new);
        if (!alert.isOwnedBy(externalProfileService.fetchProfileIdByEmail(query.requesterEmail())))
            throw new ForbiddenOperationException("No tienes permiso para ver esta alerta");
        return alert;
    }

    @Override
    public long handle(CountUnreadAlertsQuery query) {
        return alertRepository.countByRecipientIdAndStatus(
                externalProfileService.fetchProfileIdByEmail(query.requesterEmail()), AlertStatus.UNREAD);
    }

    @Override
    public AlertSettings handle(GetAlertSettingsQuery query) {
        var profileId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return settingsRepository.findByProfileId(profileId).orElseGet(() -> AlertSettings.defaultsFor(profileId));
    }
}
