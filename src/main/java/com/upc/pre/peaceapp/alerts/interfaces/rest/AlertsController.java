package com.upc.pre.peaceapp.alerts.interfaces.rest;

import com.upc.pre.peaceapp.alerts.domain.model.commands.*;
import com.upc.pre.peaceapp.alerts.domain.model.queries.*;
import com.upc.pre.peaceapp.alerts.domain.services.AlertCommandService;
import com.upc.pre.peaceapp.alerts.domain.services.AlertQueryService;
import com.upc.pre.peaceapp.alerts.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.alerts.interfaces.rest.transform.*;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.shared.interfaces.rest.AuthenticatedRequester;
import com.upc.pre.peaceapp.shared.interfaces.rest.PagedResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Alertas de proximidad del usuario autenticado. Las crea el sistema cuando se publica un reporte
 * cerca de la última ubicación del usuario; aquí solo se consultan, se marcan como leídas y se configuran.
 */
@RestController
@RequestMapping(value = "/api/v1/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Alerts", description = "Alerts Endpoints")
public class AlertsController {
    private static final int MAX_PAGE_SIZE = 100;

    private final AlertCommandService commandService;
    private final AlertQueryService queryService;

    public AlertsController(AlertCommandService commandService, AlertQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<PagedResource<AlertResource>> getMyAlerts(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        var alerts = queryService.handle(new GetAlertsQuery(authentication.getName(), unread, pageable(page, size)));
        return ResponseEntity.ok(PagedResource.from(alerts, AlertResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CountResource> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(new CountResource(
                queryService.handle(new CountUnreadAlertsQuery(authentication.getName()))));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResource<AlertResource>> getAlertsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        var alerts = queryService.handle(new GetAlertsByUserQuery(userId, authentication.getName(),
                AuthenticatedRequester.isAdmin(authentication), pageable(page, size)));
        return ResponseEntity.ok(PagedResource.from(alerts, AlertResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResource> getAlertById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(
                queryService.handle(new GetAlertByIdQuery(id, authentication.getName()))));
    }

    /** Es PUT y no PATCH porque la configuración de CORS solo permite GET, POST, PUT y DELETE. */
    @PutMapping("/{id}/read")
    public ResponseEntity<AlertResource> markRead(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(
                commandService.handle(new MarkAlertReadCommand(id, authentication.getName()))));
    }

    @PutMapping("/read-all")
    public ResponseEntity<CountResource> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(new CountResource(
                commandService.handle(new MarkAllAlertsReadCommand(authentication.getName()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new DeleteAlertCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    /** Borra solo las alertas del usuario autenticado, nunca las de otros. */
    @DeleteMapping({"", "/"})
    public ResponseEntity<Void> deleteMyAlerts(Authentication authentication) {
        commandService.handle(new DeleteMyAlertsCommand(authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<AlertSettingsResource> getSettings(Authentication authentication) {
        return ResponseEntity.ok(AlertSettingsResourceFromEntityAssembler.toResourceFromEntity(
                queryService.handle(new GetAlertSettingsQuery(authentication.getName()))));
    }

    @PutMapping("/settings")
    public ResponseEntity<AlertSettingsResource> updateSettings(@Valid @RequestBody UpdateAlertSettingsResource resource,
                                                                Authentication authentication) {
        var settings = commandService.handle(new UpdateAlertSettingsCommand(authentication.getName(),
                resource.radiusMeters(), resource.minSeverity(), resource.ignoredTypes(), resource.mutedUntil()));
        return ResponseEntity.ok(AlertSettingsResourceFromEntityAssembler.toResourceFromEntity(settings));
    }

    /** El móvil la llama cada cierto tiempo; con esta posición se decide a quién avisar de un reporte nuevo. */
    @PutMapping("/settings/location")
    public ResponseEntity<AlertSettingsResource> updateLocation(@Valid @RequestBody UpdateLocationResource resource,
                                                                Authentication authentication) {
        var settings = commandService.handle(new UpdateLastLocationCommand(authentication.getName(),
                GeoPoint.of(resource.latitude(), resource.longitude())));
        return ResponseEntity.ok(AlertSettingsResourceFromEntityAssembler.toResourceFromEntity(settings));
    }

    private static Pageable pageable(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page no puede ser negativo");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending()));
    }
}
