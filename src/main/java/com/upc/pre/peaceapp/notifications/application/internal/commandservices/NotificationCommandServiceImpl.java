package com.upc.pre.peaceapp.notifications.application.internal.commandservices;

import com.upc.pre.peaceapp.notifications.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.notifications.application.internal.outboundservices.push.PushNotificationSender;
import com.upc.pre.peaceapp.notifications.domain.exceptions.NotificationNotFoundException;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.DeviceRegistration;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.Notification;
import com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences;
import com.upc.pre.peaceapp.notifications.domain.model.commands.*;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationCommandService;
import com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories.DeviceRegistrationRepository;
import com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferencesRepository;
import com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class NotificationCommandServiceImpl implements NotificationCommandService {
    private static final Logger log = LoggerFactory.getLogger(NotificationCommandServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final DeviceRegistrationRepository deviceRepository;
    private final ExternalProfileService externalProfileService;
    private final PushNotificationSender pushSender;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                          NotificationPreferencesRepository preferencesRepository,
                                          DeviceRegistrationRepository deviceRepository,
                                          ExternalProfileService externalProfileService,
                                          PushNotificationSender pushSender) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.deviceRepository = deviceRepository;
        this.externalProfileService = externalProfileService;
        this.pushSender = pushSender;
    }

    /** Propia transacción: se dispara después de confirmar la alerta que originó el aviso. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Notification> handle(CreateNotificationCommand command) {
        var preferences = preferencesRepository.findByProfileId(command.recipientId())
                .orElseGet(() -> NotificationPreferences.defaultsFor(command.recipientId()));
        if (!preferences.allows(command.kind())) return Optional.empty();
        if (notificationRepository.existsByRecipientIdAndKindAndReferenceId(command.recipientId(), command.kind(),
                command.referenceId())) return Optional.empty();

        var notification = new Notification(command.recipientId(), command.kind(), command.title(), command.body(),
                command.referenceId());
        if (preferences.isInAppEnabled()) notification = notificationRepository.save(notification);
        if (preferences.isPushEnabled()) pushToDevices(command.recipientId(), notification);
        return preferences.isInAppEnabled() ? Optional.of(notification) : Optional.empty();
    }

    @Override
    public Notification handle(MarkNotificationReadCommand command) {
        var notification = findOwned(command.notificationId(), command.requesterEmail());
        notification.markRead(Instant.now());
        return notificationRepository.save(notification);
    }

    @Override
    public int handle(MarkAllNotificationsReadCommand command) {
        return notificationRepository.markAllRead(requesterId(command.requesterEmail()), Instant.now());
    }

    @Override
    public void handle(DeleteNotificationCommand command) {
        notificationRepository.delete(findOwned(command.notificationId(), command.requesterEmail()));
    }

    @Override
    public NotificationPreferences handle(UpdateNotificationPreferencesCommand command) {
        var profileId = requesterId(command.requesterEmail());
        var preferences = preferencesRepository.findByProfileId(profileId)
                .orElseGet(() -> NotificationPreferences.defaultsFor(profileId));
        preferences.configure(command.inAppEnabled(), command.pushEnabled(), command.disabledKinds());
        return preferencesRepository.save(preferences);
    }

    @Override
    public DeviceRegistration handle(RegisterDeviceCommand command) {
        var profileId = requesterId(command.requesterEmail());
        var device = new DeviceRegistration(profileId, command.token(), command.platform());
        var existing = deviceRepository.findByToken(device.getToken());
        if (existing.isEmpty()) return deviceRepository.save(device);
        existing.get().reassign(profileId, command.platform());
        return deviceRepository.save(existing.get());
    }

    @Override
    public void handle(UnregisterDeviceCommand command) {
        var profileId = requesterId(command.requesterEmail());
        deviceRepository.findByToken(command.token() == null ? "" : command.token().trim())
                .filter(device -> device.getProfileId().equals(profileId))
                .ifPresent(deviceRepository::delete);
    }

    /** Un fallo del proveedor push no debe impedir los demás envíos ni deshacer la notificación guardada. */
    private void pushToDevices(Long profileId, Notification notification) {
        for (var device : deviceRepository.findByProfileId(profileId)) {
            try {
                pushSender.send(device.getToken(), device.getPlatform(), notification.getTitle(), notification.getBody());
            } catch (RuntimeException ex) {
                log.warn("No se pudo enviar el push al dispositivo {}", device.getId(), ex);
            }
        }
    }

    private Long requesterId(String email) {
        return externalProfileService.fetchProfileIdByEmail(email);
    }

    private Notification findOwned(Long id, String requesterEmail) {
        var notification = notificationRepository.findById(id).orElseThrow(NotificationNotFoundException::new);
        if (!notification.isOwnedBy(requesterId(requesterEmail)))
            throw new ForbiddenOperationException("No tienes permiso para modificar esta notificación");
        return notification;
    }
}
