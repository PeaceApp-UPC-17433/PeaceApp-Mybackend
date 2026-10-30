package com.upc.pre.peaceapp.notifications.application.internal.queryservices;

import com.upc.pre.peaceapp.notifications.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.Notification;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences;
import com.upc.pre.peaceapp.notifications.domain.model.queries.*;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationQueryService;
import com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferencesRepository;
import com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final ExternalProfileService externalProfileService;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository,
                                        NotificationPreferencesRepository preferencesRepository,
                                        ExternalProfileService externalProfileService) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public Page<Notification> handle(GetNotificationsQuery query) {
        var recipientId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return query.unreadOnly()
                ? notificationRepository.findByRecipientIdAndReadAtIsNull(recipientId, query.pageable())
                : notificationRepository.findByRecipientId(recipientId, query.pageable());
    }

    @Override
    public long handle(CountUnreadNotificationsQuery query) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(
                externalProfileService.fetchProfileIdByEmail(query.requesterEmail()));
    }

    @Override
    public NotificationPreferences handle(GetNotificationPreferencesQuery query) {
        var profileId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return preferencesRepository.findByProfileId(profileId)
                .orElseGet(() -> NotificationPreferences.defaultsFor(profileId));
    }
}
