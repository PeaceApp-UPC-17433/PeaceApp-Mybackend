package com.upc.pre.peaceapp.notifications.interfaces.rest;

import com.upc.pre.peaceapp.notifications.domain.model.commands.*;
import com.upc.pre.peaceapp.notifications.domain.model.queries.*;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationCommandService;
import com.upc.pre.peaceapp.notifications.domain.services.NotificationQueryService;
import com.upc.pre.peaceapp.notifications.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.notifications.interfaces.rest.transform.NotificationResourceFromEntityAssembler;
import com.upc.pre.peaceapp.shared.interfaces.rest.PagedResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Historial de avisos, preferencias de canales y dispositivos push del usuario autenticado. */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notifications Endpoints")
public class NotificationsController {
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;

    public NotificationsController(NotificationCommandService commandService, NotificationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<PagedResource<NotificationResource>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        var notifications = queryService.handle(
                new GetNotificationsQuery(authentication.getName(), unread, pageable(page, size)));
        return ResponseEntity.ok(PagedResource.from(notifications,
                NotificationResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CountResource> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(new CountResource(
                queryService.handle(new CountUnreadNotificationsQuery(authentication.getName()))));
    }

    /** Es PUT y no PATCH porque la configuración de CORS solo permite GET, POST, PUT y DELETE. */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResource> markRead(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(
                commandService.handle(new MarkNotificationReadCommand(id, authentication.getName()))));
    }

    @PutMapping("/read-all")
    public ResponseEntity<CountResource> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(new CountResource(
                commandService.handle(new MarkAllNotificationsReadCommand(authentication.getName()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new DeleteNotificationCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResource> getPreferences(Authentication authentication) {
        return ResponseEntity.ok(toResource(
                queryService.handle(new GetNotificationPreferencesQuery(authentication.getName()))));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResource> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesResource resource, Authentication authentication) {
        return ResponseEntity.ok(toResource(commandService.handle(new UpdateNotificationPreferencesCommand(
                authentication.getName(), resource.inAppEnabled(), resource.pushEnabled(), resource.disabledKinds()))));
    }

    /** Registra el token push del dispositivo; si ya existía, pasa a ser de este usuario. */
    @PostMapping("/devices")
    public ResponseEntity<DeviceResource> registerDevice(@Valid @RequestBody RegisterDeviceResource resource,
                                                         Authentication authentication) {
        var device = commandService.handle(
                new RegisterDeviceCommand(authentication.getName(), resource.token(), resource.platform()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new DeviceResource(device.getId(), device.getPlatform()));
    }

    @DeleteMapping("/devices")
    public ResponseEntity<Void> unregisterDevice(@RequestParam String token, Authentication authentication) {
        commandService.handle(new UnregisterDeviceCommand(authentication.getName(), token));
        return ResponseEntity.noContent().build();
    }

    private static NotificationPreferencesResource toResource(
            com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences preferences) {
        return new NotificationPreferencesResource(preferences.isInAppEnabled(), preferences.isPushEnabled(),
                preferences.getDisabledKinds());
    }

    private static Pageable pageable(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page no puede ser negativo");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending()));
    }
}
