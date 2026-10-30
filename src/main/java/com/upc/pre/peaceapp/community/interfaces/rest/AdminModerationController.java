package com.upc.pre.peaceapp.community.interfaces.rest;

import com.upc.pre.peaceapp.community.domain.model.commands.ResolveAbuseFlagCommand;
import com.upc.pre.peaceapp.community.domain.model.queries.GetAbuseFlagsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommunityMetricsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetModerationLogQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.services.ModerationCommandService;
import com.upc.pre.peaceapp.community.domain.services.ModerationQueryService;
import com.upc.pre.peaceapp.community.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.community.interfaces.rest.transform.CommunityResourceAssemblers;
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
 * Panel de administración. Cada operación comprueba el rol en el servicio de aplicación y responde
 * 403 con el formato JSON habitual si quien llama no es administrador.
 */
@RestController
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin moderation", description = "Abuse flags, moderation log and quality metrics")
public class AdminModerationController {
    private static final int MAX_PAGE_SIZE = 100;

    private final ModerationCommandService commandService;
    private final ModerationQueryService queryService;

    public AdminModerationController(ModerationCommandService commandService, ModerationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /** Denuncias, las más nuevas primero. {@code status} filtra (por ejemplo {@code OPEN} para la cola pendiente). */
    @GetMapping("/flags")
    public ResponseEntity<PagedResource<AbuseFlagResource>> getFlags(@RequestParam(required = false) FlagStatus status,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     Authentication authentication) {
        var flags = queryService.handle(new GetAbuseFlagsQuery(AuthenticatedRequester.isAdmin(authentication), status,
                newestFirst(page, size)));
        return ResponseEntity.ok(PagedResource.from(flags, CommunityResourceAssemblers::toResource));
    }

    /** Es PUT y no PATCH porque la configuración de CORS solo permite GET, POST, PUT y DELETE. */
    @PutMapping("/flags/{flagId}/resolve")
    public ResponseEntity<AbuseFlagResource> resolveFlag(@PathVariable Long flagId,
                                                         @Valid @RequestBody ResolveAbuseFlagResource resource,
                                                         Authentication authentication) {
        var flag = commandService.handle(new ResolveAbuseFlagCommand(flagId, authentication.getName(),
                AuthenticatedRequester.isAdmin(authentication), resource.resolution(), resource.note()));
        return ResponseEntity.ok(CommunityResourceAssemblers.toResource(flag));
    }

    @GetMapping("/moderation-log")
    public ResponseEntity<PagedResource<ModerationActionResource>> getModerationLog(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        var log = queryService.handle(new GetModerationLogQuery(AuthenticatedRequester.isAdmin(authentication),
                newestFirst(page, size)));
        return ResponseEntity.ok(PagedResource.from(log, CommunityResourceAssemblers::toResource));
    }

    @GetMapping("/metrics")
    public ResponseEntity<CommunityMetricsResource> getMetrics(Authentication authentication) {
        return ResponseEntity.ok(CommunityResourceAssemblers.toResource(queryService.handle(
                new GetCommunityMetricsQuery(AuthenticatedRequester.isAdmin(authentication)))));
    }

    private static Pageable newestFirst(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page no puede ser negativo");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending()));
    }
}
